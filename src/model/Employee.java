package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Employee {
    private int id;
    private String name;
    private String designation;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal deductions;

    public Employee() {}

    public Employee(int id, String name, String designation, BigDecimal basicSalary, BigDecimal hra, BigDecimal da, BigDecimal deductions) {
        this.id = id;
        this.name = name;
        this.designation = designation;
        this.basicSalary = safe(basicSalary);
        this.hra = safe(hra);
        this.da = safe(da);
        this.deductions = safe(deductions);
    }

    public Employee(String name, String designation, BigDecimal basicSalary, BigDecimal hra, BigDecimal da, BigDecimal deductions) {
        this(0, name, designation, basicSalary, hra, da, deductions);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public BigDecimal getGrossSalary() {
        return safe(basicSalary).add(safe(hra)).add(safe(da));
    }

    public BigDecimal getNetSalary() {
        return getGrossSalary().subtract(safe(deductions));
    }

    public BigDecimal getNetSalaryRounded() {
        return getNetSalary().setScale(2, RoundingMode.HALF_UP);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = safe(basicSalary);
    }

    public BigDecimal getHra() {
        return hra;
    }

    public void setHra(BigDecimal hra) {
        this.hra = safe(hra);
    }

    public BigDecimal getDa() {
        return da;
    }

    public void setDa(BigDecimal da) {
        this.da = safe(da);
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = safe(deductions);
    }

    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s', designation='%s', basicSalary=%s, hra=%s, da=%s, deductions=%s, netSalary=%s}",
                id, name, designation, basicSalary, hra, da, deductions, getNetSalaryRounded());
    }
}
