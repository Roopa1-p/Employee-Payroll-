const API_BASE = location.origin + '/api';

document.getElementById('server-url').textContent = location.origin;

const form = document.getElementById('employee-form');
const resetBtn = document.getElementById('reset-btn');
const refreshBtn = document.getElementById('refresh-btn');
const tbody = document.querySelector('#employees-table tbody');

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const data = readForm();
  try {
    if (data.id) {
      await http('/employees/' + data.id, 'PUT', data);
      toast('Employee updated');
    } else {
      await http('/employees', 'POST', data);
      toast('Employee added');
    }
    resetForm();
    await loadEmployees();
  } catch (err) {
    alert('Error: ' + err.message);
  }
});

resetBtn.addEventListener('click', () => {
  resetForm();
});

refreshBtn.addEventListener('click', () => loadEmployees());

function readForm() {
  return {
    id: document.getElementById('id').value.trim(),
    name: document.getElementById('name').value.trim(),
    designation: document.getElementById('designation').value.trim(),
    basic_salary: document.getElementById('basic_salary').value.trim(),
    hra: document.getElementById('hra').value.trim(),
    da: document.getElementById('da').value.trim(),
    deductions: document.getElementById('deductions').value.trim()
  };
}

function resetForm() {
  form.reset();
  document.getElementById('id').value = '';
  document.getElementById('form-title').textContent = 'Add Employee';
  document.getElementById('save-btn').textContent = 'Save';
}

async function loadEmployees() {
  const employees = await http('/employees');
  renderRows(employees);
}

function renderRows(employees) {
  tbody.innerHTML = '';
  for (const e of employees) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${e.id}</td>
      <td>${escapeHtml(e.name)}</td>
      <td>${escapeHtml(e.designation)}</td>
      <td>${fmt(e.basic_salary)}</td>
      <td>${fmt(e.hra)}</td>
      <td>${fmt(e.da)}</td>
      <td>${fmt(e.deductions)}</td>
      <td class="pill">${fmt(e.gross_salary)}</td>
      <td class="pill">${fmt(e.net_salary)}</td>
      <td class="actions">
        <button class="secondary" data-action="edit">Edit</button>
        <button data-action="delete">Delete</button>
      </td>
    `;
    tr.querySelector('[data-action="edit"]').addEventListener('click', () => fillForm(e));
    tr.querySelector('[data-action="delete"]').addEventListener('click', async () => {
      if (!confirm('Delete employee #' + e.id + '?')) return;
      await http('/employees/' + e.id, 'DELETE');
      toast('Employee deleted');
      await loadEmployees();
    });
    tbody.appendChild(tr);
  }
}

function fillForm(e) {
  document.getElementById('id').value = e.id;
  document.getElementById('name').value = e.name;
  document.getElementById('designation').value = e.designation;
  document.getElementById('basic_salary').value = e.basic_salary;
  document.getElementById('hra').value = e.hra;
  document.getElementById('da').value = e.da;
  document.getElementById('deductions').value = e.deductions;
  document.getElementById('form-title').textContent = 'Edit Employee #' + e.id;
  document.getElementById('save-btn').textContent = 'Update';
}

async function http(path, method = 'GET', body) {
  const headers = {};
  let payload;
  if (body) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(API_BASE + path, { method, headers, body: payload });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  const ct = res.headers.get('Content-Type') || '';
  if (ct.includes('application/json')) return res.json();
  return res.text();
}

function fmt(n) {
  return Number(n).toFixed(2);
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[c]));
}

function toast(msg) {
  console.log(msg);
}

loadEmployees().catch(err => console.error(err));
