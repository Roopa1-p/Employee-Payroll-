package web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dao.EmployeeDAO;
import model.Employee;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

public class PayrollServer {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null) {
            try { port = Integer.parseInt(envPort); } catch (NumberFormatException ignored) {}
        }
        PayrollServer app = new PayrollServer();
        app.start(port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/employees", new EmployeeApiHandler(employeeDAO));
        server.createContext("/", new StaticFileHandler("public"));
        server.setExecutor(null);
        System.out.println("Server started on http://localhost:" + port);
        server.start();
    }

    static class EmployeeApiHandler implements HttpHandler {
        private final EmployeeDAO employeeDAO;

        EmployeeApiHandler(EmployeeDAO employeeDAO) { this.employeeDAO = employeeDAO; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                URI uri = exchange.getRequestURI();
                String path = uri.getPath();
                if (path == null) path = "/api/employees";

                if (path.equals("/api/employees") || path.equals("/api/employees/")) {
                    if (method.equals("GET")) {
                        handleList(exchange);
                    } else if (method.equals("POST")) {
                        handleCreate(exchange);
                    } else {
                        sendText(exchange, 405, "Method Not Allowed");
                    }
                    return;
                }

                // Expecting /api/employees/{id}
                String[] parts = path.split("/");
                if (parts.length >= 4) {
                    String idPart = parts[3];
                    int id;
                    try { id = Integer.parseInt(idPart); }
                    catch (NumberFormatException e) { sendText(exchange, 400, "Invalid ID"); return; }

                    if (method.equals("GET")) {
                        handleGetOne(exchange, id);
                    } else if (method.equals("PUT")) {
                        handleUpdate(exchange, id);
                    } else if (method.equals("DELETE")) {
                        handleDelete(exchange, id);
                    } else {
                        sendText(exchange, 405, "Method Not Allowed");
                    }
                    return;
                }

                sendText(exchange, 404, "Not Found");
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            } finally {
                exchange.close();
            }
        }

        private void handleList(HttpExchange exchange) throws IOException, SQLException {
            List<Employee> employees = employeeDAO.getAllEmployees();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < employees.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(JsonUtil.employeeToJson(employees.get(i)));
            }
            sb.append("]");
            sendJson(exchange, 200, sb.toString());
        }

        private void handleGetOne(HttpExchange exchange, int id) throws IOException, SQLException {
            Employee employee = employeeDAO.getEmployeeById(id);
            if (employee == null) { sendText(exchange, 404, "Not Found"); return; }
            sendJson(exchange, 200, JsonUtil.employeeToJson(employee));
        }

        private void handleCreate(HttpExchange exchange) throws IOException, SQLException {
            Map<String, String> params = readParams(exchange);
            Employee employee = readEmployeeFromParams(params, 0);
            employeeDAO.addEmployee(employee);
            sendJson(exchange, 201, JsonUtil.employeeToJson(employee));
        }

        private void handleUpdate(HttpExchange exchange, int id) throws IOException, SQLException {
            Employee existing = employeeDAO.getEmployeeById(id);
            if (existing == null) { sendText(exchange, 404, "Not Found"); return; }
            Map<String, String> params = readParams(exchange);
            // Allow partial update: if a field missing, keep existing
            String name = params.getOrDefault("name", existing.getName());
            String designation = params.getOrDefault("designation", existing.getDesignation());
            BigDecimal basic = parseDecimalOrDefault(params.get("basic_salary"), existing.getBasicSalary());
            BigDecimal hra = parseDecimalOrDefault(params.get("hra"), existing.getHra());
            BigDecimal da = parseDecimalOrDefault(params.get("da"), existing.getDa());
            BigDecimal deductions = parseDecimalOrDefault(params.get("deductions"), existing.getDeductions());

            existing.setName(name);
            existing.setDesignation(designation);
            existing.setBasicSalary(basic);
            existing.setHra(hra);
            existing.setDa(da);
            existing.setDeductions(deductions);
            employeeDAO.updateEmployee(existing);
            sendJson(exchange, 200, JsonUtil.employeeToJson(existing));
        }

        private void handleDelete(HttpExchange exchange, int id) throws IOException, SQLException {
            boolean deleted = employeeDAO.deleteEmployee(id);
            if (!deleted) { sendText(exchange, 404, "Not Found"); return; }
            sendJson(exchange, 200, "{\"status\":\"deleted\"}");
        }

        private Map<String, String> readParams(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getRequestHeaders();
            String contentType = headers.getFirst("Content-Type");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                return parseUrlEncoded(body);
            }
            if (contentType != null && contentType.contains("application/json")) {
                return parseSimpleJson(body);
            }
            // Fallback try urlencoded
            return parseUrlEncoded(body);
        }

        private Map<String, String> parseUrlEncoded(String body) {
            Map<String, String> params = new HashMap<>();
            if (body == null || body.isEmpty()) return params;
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx < 0) continue;
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
            return params;
        }

        private Map<String, String> parseSimpleJson(String body) {
            // Very naive parser expecting flat JSON object with simple string/number values.
            Map<String, String> params = new HashMap<>();
            if (body == null) return params;
            String trimmed = body.trim();
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return params;
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            if (trimmed.isEmpty()) return params;
            String[] pairs = trimmed.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length != 2) continue;
                String key = stripQuotes(kv[0].trim());
                String value = stripQuotes(kv[1].trim());
                params.put(key, value);
            }
            return params;
        }

        private String stripQuotes(String s) {
            if (s.startsWith("\"") && s.endsWith("\"")) {
                return s.substring(1, s.length() - 1);
            }
            return s;
        }

        private Employee readEmployeeFromParams(Map<String, String> p, int idIfAny) throws IOException {
            String name = require(p, "name");
            String designation = require(p, "designation");
            BigDecimal basic = parseDecimal(require(p, "basic_salary"));
            BigDecimal hra = parseDecimal(require(p, "hra"));
            BigDecimal da = parseDecimal(require(p, "da"));
            BigDecimal deductions = parseDecimal(require(p, "deductions"));
            if (idIfAny > 0) {
                return new Employee(idIfAny, name, designation, basic, hra, da, deductions);
            }
            return new Employee(name, designation, basic, hra, da, deductions);
        }

        private String require(Map<String, String> p, String key) throws IOException {
            String v = p.get(key);
            if (v == null || v.isEmpty()) throw new IOException("Missing parameter: " + key);
            return v;
        }

        private BigDecimal parseDecimal(String s) throws IOException {
            try { return new BigDecimal(s); }
            catch (Exception e) { throw new IOException("Invalid number for value: " + s); }
        }

        private BigDecimal parseDecimalOrDefault(String s, BigDecimal def) {
            if (s == null || s.isEmpty()) return def;
            try { return new BigDecimal(s); } catch (Exception e) { return def; }
        }
    }

    static class JsonUtil {
        static String escape(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int)c));
                        else sb.append(c);
                }
            }
            return sb.toString();
        }

        static String quote(String s) { return "\"" + escape(s) + "\""; }

        static String employeeToJson(Employee e) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"id\":").append(e.getId()).append(',');
            sb.append("\"name\":").append(quote(e.getName())).append(',');
            sb.append("\"designation\":").append(quote(e.getDesignation())).append(',');
            sb.append("\"basic_salary\":").append(num(e.getBasicSalary())).append(',');
            sb.append("\"hra\":").append(num(e.getHra())).append(',');
            sb.append("\"da\":").append(num(e.getDa())).append(',');
            sb.append("\"deductions\":").append(num(e.getDeductions())).append(',');
            sb.append("\"gross_salary\":").append(num(e.getGrossSalary())).append(',');
            sb.append("\"net_salary\":").append(num(e.getNetSalaryRounded()));
            sb.append("}");
            return sb.toString();
        }

        static String num(BigDecimal n) { return n == null ? "0" : n.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
    }

    static class StaticFileHandler implements HttpHandler {
        private final Path basePath;

        StaticFileHandler(String baseDir) {
            this.basePath = Path.of(System.getProperty("user.dir"), baseDir).normalize();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            URI uri = exchange.getRequestURI();
            String requestPath = uri.getPath();
            if (requestPath == null || requestPath.equals("/")) requestPath = "/index.html";

            String sanitized = sanitizePath(requestPath);
            Path filePath = basePath.resolve(sanitized).normalize();
            if (!filePath.startsWith(basePath) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendText(exchange, 404, "Not Found");
                return;
            }

            String contentType = contentType(filePath.toString());
            byte[] content = Files.readAllBytes(filePath);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(content); }
        }

        private String sanitizePath(String path) {
            String p = path.replace('\\', '/');
            while (p.contains("..")) {
                p = p.replace("..", "");
            }
            if (p.startsWith("/")) p = p.substring(1);
            return p;
        }

        private String contentType(String filename) {
            String f = filename.toLowerCase(Locale.ROOT);
            if (f.endsWith(".html")) return "text/html; charset=utf-8";
            if (f.endsWith(".css")) return "text/css; charset=utf-8";
            if (f.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (f.endsWith(".json")) return "application/json; charset=utf-8";
            if (f.endsWith(".svg")) return "image/svg+xml";
            if (f.endsWith(".png")) return "image/png";
            return "application/octet-stream";
        }
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
