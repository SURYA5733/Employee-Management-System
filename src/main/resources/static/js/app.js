/**
 * Employee Management System - Client Application with User-Based Access Control (UBAC/RBAC)
 */

const API_BASE = '/api/v1';

// State Management
const state = {
    auth: {
        token: localStorage.getItem('ems_jwt') || null,
        username: localStorage.getItem('ems_user') || null,
        roles: JSON.parse(localStorage.getItem('ems_roles') || '[]'),
        employeeId: localStorage.getItem('ems_emp_id') || null
    },
    currentTab: 'dashboard',
    employees: [],
    departments: [],
    stats: null,
    pagination: {
        page: 0,
        size: 10,
        totalPages: 0,
        totalElements: 0,
        sortBy: 'id',
        direction: 'desc'
    },
    filters: {
        keyword: '',
        departmentId: '',
        status: ''
    },
    activeEmployeeId: null,
    activeDeleteTarget: null // { type: 'employee' | 'department', id: number, name: string }
};

// RBAC Role Helpers
function isAdmin() {
    return state.auth.roles && state.auth.roles.includes('ROLE_ADMIN');
}

function isHr() {
    return state.auth.roles && state.auth.roles.includes('ROLE_HR');
}

function isManager() {
    return state.auth.roles && state.auth.roles.includes('ROLE_MANAGER');
}

function isEmployee() {
    // Pure employee without administrative, HR, or manager privilege
    return !isAdmin() && !isHr() && !isManager();
}

function canViewDashboard() {
    return isAdmin() || isHr();
}

function canViewAllEmployees() {
    return isAdmin() || isHr() || isManager();
}

function canAddEmployees() {
    return isAdmin() || isHr();
}

function canEditEmployees() {
    return isAdmin() || isHr() || isManager();
}

function canDeleteEmployees() {
    return isAdmin();
}

function canManageDepartments() {
    return isAdmin() || isHr();
}

function canDeleteDepartments() {
    return isAdmin();
}

function canExportCsv() {
    return isAdmin() || isHr();
}

// Authenticated Fetch Wrapper
async function authFetch(url, options = {}) {
    const headers = options.headers ? { ...options.headers } : {};

    if (state.auth.token) {
        headers['Authorization'] = `Bearer ${state.auth.token}`;
    }

    const res = await fetch(url, { ...options, headers });

    if (res.status === 401) {
        handleLogout();
        showToast('Session expired or unauthorized. Please log in.', 'error');
        throw new Error('Unauthorized');
    }

    if (res.status === 403) {
        showToast('Access Denied: You do not have permission for this action.', 'error');
        throw new Error('Forbidden');
    }

    return res;
}

// DOM Elements
const elements = {
    tabs: document.querySelectorAll('.nav-item'),
    tabContents: document.querySelectorAll('.tab-content'),
    navTabDashboard: document.getElementById('nav-tab-dashboard'),
    navTabEmployees: document.getElementById('nav-tab-employees'),
    navTabDepartments: document.getElementById('nav-tab-departments'),
    navEmployeesLabel: document.getElementById('nav-employees-label'),
    pageHeading: document.getElementById('page-heading'),
    pageSubheading: document.getElementById('page-subheading'),

    // Auth
    modalLogin: document.getElementById('modal-login'),
    formLogin: document.getElementById('form-login'),
    loginUsername: document.getElementById('login-username'),
    loginPassword: document.getElementById('login-password'),
    authUserAvatar: document.getElementById('auth-user-avatar'),
    authUserName: document.getElementById('auth-user-name'),
    authUserRole: document.getElementById('auth-user-role'),
    btnLogout: document.getElementById('btn-logout'),

    // Quick Login Demo Buttons
    btnQuickAdmin: document.getElementById('btn-quick-admin'),
    btnQuickHr: document.getElementById('btn-quick-hr'),
    btnQuickManager: document.getElementById('btn-quick-manager'),
    btnQuickEmployee: document.getElementById('btn-quick-employee'),

    // Dashboard
    statTotalEmp: document.getElementById('stat-total-emp'),
    statActiveEmp: document.getElementById('stat-active-emp'),
    statLeaveEmp: document.getElementById('stat-leave-emp'),
    statTotalDept: document.getElementById('stat-total-dept'),
    statAvgSalary: document.getElementById('stat-avg-salary'),
    statTotalPayroll: document.getElementById('stat-total-payroll'),
    deptBarsContainer: document.getElementById('department-bars-container'),

    // Employee List
    employeeViewNotice: document.getElementById('employee-view-notice'),
    employeesTableBody: document.getElementById('employees-table-body'),
    inputSearch: document.getElementById('input-search'),
    filterDepartment: document.getElementById('filter-department'),
    filterStatus: document.getElementById('filter-status'),
    filterSort: document.getElementById('filter-sort'),
    btnRefreshEmployees: document.getElementById('btn-refresh-employees'),
    paginationInfo: document.getElementById('pagination-info'),
    pageDisplay: document.getElementById('page-display'),
    btnPrevPage: document.getElementById('btn-prev-page'),
    btnNextPage: document.getElementById('btn-next-page'),
    btnExportCsv: document.getElementById('btn-export-csv'),

    // Departments
    departmentsCardsContainer: document.getElementById('departments-cards-container'),

    // Modals
    modalEmployee: document.getElementById('modal-employee'),
    modalDept: document.getElementById('modal-department'),
    modalViewEmployee: document.getElementById('modal-view-employee'),
    modalDelete: document.getElementById('modal-delete'),
    modalApplyLeave: document.getElementById('modal-apply-leave'),
    modalRejectLeave: document.getElementById('modal-reject-leave'),

    // Forms
    formEmployee: document.getElementById('form-employee'),
    formDepartment: document.getElementById('form-department'),
    formApplyLeave: document.getElementById('form-apply-leave'),
    formRejectLeave: document.getElementById('form-reject-leave'),
    modalEmployeeTitle: document.getElementById('modal-employee-title'),
    modalDeptTitle: document.getElementById('modal-dept-title'),
    btnConfirmDelete: document.getElementById('btn-confirm-delete'),
    deleteMessage: document.getElementById('delete-message'),
    viewEmployeeContent: document.getElementById('view-employee-content'),

    // Leave Elements
    navTabLeaves: document.getElementById('nav-tab-leaves'),
    leavesSectionTitle: document.getElementById('leaves-section-title'),
    leavesSectionSub: document.getElementById('leaves-section-sub'),
    leaveAdminStatsContainer: document.getElementById('leave-admin-stats-container'),
    statLeavePending: document.getElementById('stat-leave-pending'),
    statLeaveOnleave: document.getElementById('stat-leave-onleave'),
    statLeaveApproved: document.getElementById('stat-leave-approved'),
    statLeaveRejected: document.getElementById('stat-leave-rejected'),
    leaveFilterBar: document.getElementById('leave-filter-bar'),
    filterLeaveStatus: document.getElementById('filter-leave-status'),
    leavesTableBody: document.getElementById('leaves-table-body'),
    btnOpenApplyLeaveModal: document.getElementById('btn-open-apply-leave-modal'),
    leaveType: document.getElementById('leave-type'),
    leaveStartDate: document.getElementById('leave-start-date'),
    leaveEndDate: document.getElementById('leave-end-date'),
    leaveDurationPreview: document.getElementById('leave-duration-preview'),
    leaveDurationDays: document.getElementById('leave-duration-days'),
    leaveReason: document.getElementById('leave-reason'),
    rejectLeaveId: document.getElementById('reject-leave-id'),
    rejectComments: document.getElementById('reject-comments'),

    // Buttons
    btnOpenAddModal: document.getElementById('btn-open-add-modal'),
    btnOpenDeptModal: document.getElementById('btn-open-dept-modal'),
    tileAddEmp: document.getElementById('tile-add-emp'),
    tileAddDept: document.getElementById('tile-add-dept'),
    btnEditFromView: document.getElementById('btn-edit-from-view'),

    // Toast
    toastContainer: document.getElementById('toast-container')
};

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    initEventListeners();

    if (!state.auth.token) {
        openLoginModal();
    } else {
        updateAuthUserUI();
        applyRbacUI();
        if (canViewDashboard()) {
            loadDashboardStats();
            loadDepartments();
        }
        loadEmployees();
    }
});

// Event Listeners Setup
function initEventListeners() {
    // Navigation Tabs
    elements.tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const tabName = tab.getAttribute('data-tab');
            switchTab(tabName);
        });
    });

    // Auth actions
    if (elements.btnLogout) {
        elements.btnLogout.addEventListener('click', handleLogout);
    }
    if (elements.formLogin) {
        elements.formLogin.addEventListener('submit', handleLoginFormSubmit);
    }

    // Quick Login Demo Buttons
    if (elements.btnQuickAdmin) {
        elements.btnQuickAdmin.addEventListener('click', () => doLogin('admin', 'admin123'));
    }
    if (elements.btnQuickHr) {
        elements.btnQuickHr.addEventListener('click', () => doLogin('hr', 'hr123'));
    }
    if (elements.btnQuickManager) {
        elements.btnQuickManager.addEventListener('click', () => doLogin('manager', 'manager123'));
    }
    if (elements.btnQuickEmployee) {
        elements.btnQuickEmployee.addEventListener('click', () => doLogin('user', 'user123'));
    }

    // Quick Action Tiles
    if (elements.tileAddEmp) {
        elements.tileAddEmp.addEventListener('click', () => {
            if (canAddEmployees()) openEmployeeModal();
            else showToast('Access Denied: Only HR and Admins can add employees', 'error');
        });
    }
    if (elements.tileAddDept) {
        elements.tileAddDept.addEventListener('click', () => {
            if (canManageDepartments()) openDepartmentModal();
            else showToast('Access Denied: Only HR and Admins can manage departments', 'error');
        });
    }

    // Modal Triggers
    if (elements.btnOpenAddModal) {
        elements.btnOpenAddModal.addEventListener('click', () => {
            if (canAddEmployees()) openEmployeeModal();
            else showToast('Access Denied: Only HR and Admins can add employees', 'error');
        });
    }
    if (elements.btnOpenDeptModal) {
        elements.btnOpenDeptModal.addEventListener('click', () => {
            if (canManageDepartments()) openDepartmentModal();
            else showToast('Access Denied: Only HR and Admins can manage departments', 'error');
        });
    }

    // Modal Close Buttons
    document.querySelectorAll('[data-close-modal]').forEach(btn => {
        btn.addEventListener('click', () => {
            const modalId = btn.getAttribute('data-close-modal');
            closeModal(modalId);
        });
    });

    // Leave Management Listeners
    if (elements.btnOpenApplyLeaveModal) {
        elements.btnOpenApplyLeaveModal.addEventListener('click', openApplyLeaveModal);
    }
    if (elements.formApplyLeave) {
        elements.formApplyLeave.addEventListener('submit', handleApplyLeaveSubmit);
    }
    if (elements.formRejectLeave) {
        elements.formRejectLeave.addEventListener('submit', handleRejectLeaveSubmit);
    }
    if (elements.leaveStartDate) {
        elements.leaveStartDate.addEventListener('change', updateDurationPreview);
    }
    if (elements.leaveEndDate) {
        elements.leaveEndDate.addEventListener('change', updateDurationPreview);
    }
    if (elements.filterLeaveStatus) {
        elements.filterLeaveStatus.addEventListener('change', () => {
            if (!isEmployee()) loadAllLeaves();
        });
    }

    // Close on background click (except login modal when unauthenticated)
    document.querySelectorAll('.modal-backdrop').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal && modal.id !== 'modal-login') {
                modal.classList.remove('open');
            }
        });
    });

    // Search and Filters
    let debounceTimer;
    elements.inputSearch.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            state.filters.keyword = e.target.value.trim();
            state.pagination.page = 0;
            loadEmployees();
        }, 350);
    });

    elements.filterDepartment.addEventListener('change', (e) => {
        state.filters.departmentId = e.target.value;
        state.pagination.page = 0;
        loadEmployees();
    });

    elements.filterStatus.addEventListener('change', (e) => {
        state.filters.status = e.target.value;
        state.pagination.page = 0;
        loadEmployees();
    });

    elements.filterSort.addEventListener('change', (e) => {
        const [sortBy, direction] = e.target.value.split(',');
        state.pagination.sortBy = sortBy;
        state.pagination.direction = direction;
        loadEmployees();
    });

    elements.btnRefreshEmployees.addEventListener('click', () => {
        loadEmployees();
        showToast('Employee list refreshed', 'info');
    });

    // Pagination
    elements.btnPrevPage.addEventListener('click', () => {
        if (state.pagination.page > 0) {
            state.pagination.page--;
            loadEmployees();
        }
    });

    elements.btnNextPage.addEventListener('click', () => {
        if (state.pagination.page < state.pagination.totalPages - 1) {
            state.pagination.page++;
            loadEmployees();
        }
    });

    // Export CSV (Admin & HR only)
    elements.btnExportCsv.addEventListener('click', async () => {
        if (!canExportCsv()) {
            showToast('Access Denied: Export restricted to HR and Admins', 'error');
            return;
        }
        try {
            const res = await authFetch(`${API_BASE}/employees/export/csv`);
            if (res.ok) {
                const blob = await res.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'employees.csv';
                document.body.appendChild(a);
                a.click();
                a.remove();
                showToast('CSV roster downloaded successfully', 'success');
            }
        } catch (err) {
            console.error('CSV Export Error:', err);
        }
    });

    // Form Submissions
    elements.formEmployee.addEventListener('submit', handleEmployeeFormSubmit);
    elements.formDepartment.addEventListener('submit', handleDepartmentFormSubmit);
    elements.btnConfirmDelete.addEventListener('click', handleConfirmDelete);

    if (elements.btnEditFromView) {
        elements.btnEditFromView.addEventListener('click', () => {
            closeModal('modal-view-employee');
            if (state.activeEmployeeId) {
                const emp = state.employees.find(e => e.id === state.activeEmployeeId);
                if (emp) openEmployeeModal(emp);
            }
        });
    }
}

// Authentication Handlers
function openLoginModal() {
    if (elements.modalLogin) {
        elements.modalLogin.classList.add('open');
    }
}

async function doLogin(usernameOrEmail, password) {
    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ usernameOrEmail, password })
        });

        const json = await res.json();

        if (res.ok && json.success) {
            const authData = json.data;
            state.auth.token = authData.token;
            state.auth.username = authData.username;
            state.auth.roles = authData.roles;
            state.auth.employeeId = authData.employeeId;

            localStorage.setItem('ems_jwt', authData.token);
            localStorage.setItem('ems_user', authData.username);
            localStorage.setItem('ems_roles', JSON.stringify(authData.roles));
            if (authData.employeeId) {
                localStorage.setItem('ems_emp_id', authData.employeeId);
            }

            closeModal('modal-login');
            updateAuthUserUI();
            applyRbacUI();
            showToast(`Welcome back, ${authData.username}!`, 'success');

            if (canViewDashboard()) {
                switchTab('dashboard');
                loadDepartments();
                loadDashboardStats();
            } else {
                switchTab('employees');
            }
            loadEmployees();
        } else {
            showToast(json.message || 'Invalid username or password', 'error');
        }
    } catch (err) {
        console.error('Login error:', err);
        showToast('Login failed. Please check backend connection.', 'error');
    }
}

async function handleLoginFormSubmit(e) {
    e.preventDefault();
    const u = elements.loginUsername.value.trim();
    const p = elements.loginPassword.value;
    await doLogin(u, p);
}

function handleLogout() {
    state.auth = { token: null, username: null, roles: [], employeeId: null };
    localStorage.removeItem('ems_jwt');
    localStorage.removeItem('ems_user');
    localStorage.removeItem('ems_roles');
    localStorage.removeItem('ems_emp_id');
    openLoginModal();
    showToast('Logged out successfully', 'info');
}

function updateAuthUserUI() {
    if (!state.auth.username) return;
    elements.authUserName.textContent = state.auth.username;
    elements.authUserAvatar.textContent = state.auth.username.substring(0, 2).toUpperCase();

    let primaryRole = 'EMPLOYEE';
    let roleClass = 'badge-employee';

    if (isAdmin()) {
        primaryRole = 'ADMIN';
        roleClass = 'badge-admin';
    } else if (isHr()) {
        primaryRole = 'HR USER';
        roleClass = 'badge-hr';
    } else if (isManager()) {
        primaryRole = 'MANAGER';
        roleClass = 'badge-manager';
    }

    elements.authUserRole.textContent = primaryRole;
    elements.authUserRole.className = `user-role-badge ${roleClass}`;
}

// User-Based Access Control UI Guard
function applyRbacUI() {
    const isEmp = isEmployee();
    const isMgr = isManager();
    const isHumanRes = isHr();
    const isAdm = isAdmin();

    // 1. Dashboard Tab Visibility (Admin and HR only)
    if (elements.navTabDashboard) {
        elements.navTabDashboard.style.display = canViewDashboard() ? 'flex' : 'none';
    }

    // 2. Departments Tab Visibility (Admin and HR only)
    if (elements.navTabDepartments) {
        elements.navTabDepartments.style.display = canManageDepartments() ? 'flex' : 'none';
    }

    // 3. Navigation labels and notices for Employee
    if (isEmp) {
        if (elements.navEmployeesLabel) elements.navEmployeesLabel.textContent = 'My Profile';
        if (elements.employeeViewNotice) elements.employeeViewNotice.style.display = 'flex';
        if (state.currentTab === 'dashboard' || state.currentTab === 'departments') {
            switchTab('employees');
        }
    } else {
        if (elements.navEmployeesLabel) elements.navEmployeesLabel.textContent = 'Employees';
        if (elements.employeeViewNotice) elements.employeeViewNotice.style.display = 'none';
    }

    // 4. If Manager logs in, ensure not on restricted tabs
    if (isMgr && (state.currentTab === 'dashboard' || state.currentTab === 'departments')) {
        switchTab('employees');
    }

    // 5. Add Employee Button (Admin and HR only; Manager & Employee cannot add)
    if (elements.btnOpenAddModal) {
        elements.btnOpenAddModal.style.display = canAddEmployees() ? 'inline-flex' : 'none';
    }

    // 6. Export CSV Button (Admin and HR only)
    if (elements.btnExportCsv) {
        elements.btnExportCsv.style.display = canExportCsv() ? 'inline-flex' : 'none';
    }

    // 7. Add Department Button (Admin and HR only)
    if (elements.btnOpenDeptModal) {
        elements.btnOpenDeptModal.style.display = canManageDepartments() ? 'inline-flex' : 'none';
    }

    // 8. Quick Action Tiles in Dashboard
    if (elements.tileAddEmp) {
        elements.tileAddEmp.style.display = canAddEmployees() ? 'flex' : 'none';
    }
    if (elements.tileAddDept) {
        elements.tileAddDept.style.display = canManageDepartments() ? 'flex' : 'none';
    }

    // 9. Edit button inside Profile View Modal
    if (elements.btnEditFromView) {
        elements.btnEditFromView.style.display = canEditEmployees() ? 'inline-flex' : 'none';
    }

    // 10. Leave Tab View Configuration
    if (elements.navTabLeaves) {
        elements.navTabLeaves.style.display = 'flex';
    }
    if (isEmp) {
        if (elements.leaveAdminStatsContainer) elements.leaveAdminStatsContainer.style.display = 'none';
        if (elements.leaveFilterBar) elements.leaveFilterBar.style.display = 'none';
        if (elements.leavesSectionTitle) elements.leavesSectionTitle.textContent = 'My Leave Applications';
        if (elements.leavesSectionSub) elements.leavesSectionSub.textContent = 'Apply for leave, track approval status, or cancel pending requests';
    } else {
        if (elements.leaveAdminStatsContainer) elements.leaveAdminStatsContainer.style.display = 'grid';
        if (elements.leaveFilterBar) elements.leaveFilterBar.style.display = 'flex';
        if (elements.leavesSectionTitle) elements.leavesSectionTitle.textContent = 'Company Leave Requests & Approvals';
        if (elements.leavesSectionSub) elements.leavesSectionSub.textContent = 'Review team leave applications, approve time off, and track absence records';
    }
}

// Tab Switching
function switchTab(tabName) {
    // Prevent unauthorized navigation to restricted tabs
    if (tabName === 'dashboard' && !canViewDashboard()) {
        showToast('Access Denied: Only HR and Admins can view the HR dashboard', 'error');
        return;
    }
    if (tabName === 'departments' && !canManageDepartments()) {
        showToast('Access Denied: Only HR and Admins can manage departments', 'error');
        return;
    }

    state.currentTab = tabName;
    elements.tabs.forEach(t => {
        if (t.getAttribute('data-tab') === tabName) {
            t.classList.add('active');
        } else {
            t.classList.remove('active');
        }
    });

    elements.tabContents.forEach(content => {
        if (content.id === `tab-${tabName}`) {
            content.classList.add('active');
        } else {
            content.classList.remove('active');
        }
    });

    if (tabName === 'dashboard') {
        elements.pageHeading.textContent = 'HR & Operations Dashboard';
        elements.pageSubheading.textContent = 'Manage team members, headcount metrics, and payroll performance';
        loadDashboardStats();
    } else if (tabName === 'employees') {
        if (isEmployee()) {
            elements.pageHeading.textContent = 'My Employee Account';
            elements.pageSubheading.textContent = 'View your personal employee profile, department, and role';
        } else {
            elements.pageHeading.textContent = 'Employees Directory';
            elements.pageSubheading.textContent = 'Search, filter, view and manage company staff records';
        }
        loadEmployees();
    } else if (tabName === 'departments') {
        elements.pageHeading.textContent = 'Departments Directory';
        elements.pageSubheading.textContent = 'Manage company business units and office locations';
        loadDepartments();
    } else if (tabName === 'leaves') {
        if (isEmployee()) {
            elements.pageHeading.textContent = 'Leave Applications & Balance';
            elements.pageSubheading.textContent = 'Submit new leave requests and track your approval status';
            loadMyLeaves();
        } else {
            elements.pageHeading.textContent = 'Leave Approvals & Tracking';
            elements.pageSubheading.textContent = 'Review team leave applications, approve time off, and track absence records';
            loadLeaveStats();
            loadAllLeaves();
        }
    }
}

// Fetch & Render Dashboard Stats
async function loadDashboardStats() {
    if (!canViewDashboard()) return;

    try {
        const res = await authFetch(`${API_BASE}/dashboard/stats`);
        const json = await res.json();
        if (json.success) {
            const data = json.data;
            state.stats = data;

            elements.statTotalEmp.textContent = data.totalEmployees || 0;
            elements.statActiveEmp.textContent = data.activeEmployees || 0;
            elements.statLeaveEmp.textContent = data.onLeaveEmployees || 0;
            elements.statTotalDept.textContent = data.totalDepartments || 0;
            elements.statAvgSalary.textContent = formatCurrency(data.averageSalary);
            elements.statTotalPayroll.textContent = formatCurrency(data.totalPayroll);

            renderDepartmentBars(data.departmentDistribution, data.totalEmployees);
        }
    } catch (err) {
        console.error('Failed to load dashboard stats:', err);
    }
}

function renderDepartmentBars(dist, total) {
    if (!elements.deptBarsContainer) return;
    if (!dist || Object.keys(dist).length === 0) {
        elements.deptBarsContainer.innerHTML = '<p class="text-muted">No department distribution data available.</p>';
        return;
    }

    let html = '';
    const max = total > 0 ? total : 1;
    for (const [deptName, count] of Object.entries(dist)) {
        const pct = Math.round((count / max) * 100);
        html += `
            <div class="dept-bar-item">
                <div class="dept-bar-info">
                    <strong>${escapeHtml(deptName)}</strong>
                    <span>${count} (${pct}%)</span>
                </div>
                <div class="dept-bar-track">
                    <div class="dept-bar-fill" style="width: ${pct}%"></div>
                </div>
            </div>
        `;
    }
    elements.deptBarsContainer.innerHTML = html;
}

// Fetch & Render Departments
async function loadDepartments() {
    if (!canManageDepartments()) return;

    try {
        const res = await authFetch(`${API_BASE}/departments`);
        const json = await res.json();
        if (json.success) {
            state.departments = json.data;
            renderDepartmentCards(json.data);
            populateDepartmentDropdowns(json.data);
        }
    } catch (err) {
        console.error('Failed to load departments:', err);
    }
}

function renderDepartmentCards(departments) {
    if (!elements.departmentsCardsContainer) return;
    if (departments.length === 0) {
        elements.departmentsCardsContainer.innerHTML = '<p class="text-muted">No departments created yet.</p>';
        return;
    }

    // Only Admin can delete departments; HR manages/adds/edits
    const showDelete = canDeleteDepartments();

    elements.departmentsCardsContainer.innerHTML = departments.map(d => `
        <div class="dept-card">
            <div class="dept-card-top">
                <span class="dept-badge-code">${escapeHtml(d.code)}</span>
                ${showDelete ? `
                <button class="icon-btn danger" onclick="confirmDeleteDepartment(${d.id}, '${escapeHtml(d.name)}')" title="Delete Department (Admin only)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                </button>` : ''}
            </div>
            <h4>${escapeHtml(d.name)}</h4>
            <p>${escapeHtml(d.description || 'No description provided.')}</p>
            <div class="dept-card-meta">
                <span>📍 ${escapeHtml(d.location || 'HQ')}</span>
                <strong>${d.employeeCount || 0} Staff</strong>
            </div>
        </div>
    `).join('');
}

function populateDepartmentDropdowns(departments) {
    const currentFilterVal = elements.filterDepartment.value;
    elements.filterDepartment.innerHTML = '<option value="">All Departments</option>' +
        departments.map(d => `<option value="${d.id}">${escapeHtml(d.name)} (${escapeHtml(d.code)})</option>`).join('');
    elements.filterDepartment.value = currentFilterVal;

    const empDeptSelect = document.getElementById('emp-department');
    if (empDeptSelect) {
        empDeptSelect.innerHTML = '<option value="">Select Department...</option>' +
            departments.map(d => `<option value="${d.id}">${escapeHtml(d.name)} (${escapeHtml(d.code)})</option>`).join('');
    }
}

// Fetch & Render Employees
async function loadEmployees() {
    try {
        elements.employeesTableBody.innerHTML = `<tr><td colspan="7" class="loading-cell">Loading employee data...</td></tr>`;

        const params = new URLSearchParams({
            page: state.pagination.page,
            size: state.pagination.size,
            sortBy: state.pagination.sortBy,
            direction: state.pagination.direction
        });

        if (state.filters.keyword) params.append('keyword', state.filters.keyword);
        if (state.filters.departmentId) params.append('departmentId', state.filters.departmentId);
        if (state.filters.status) params.append('status', state.filters.status);

        const res = await authFetch(`${API_BASE}/employees?${params.toString()}`);
        const json = await res.json();

        if (json.success) {
            const pageData = json.data;
            state.employees = pageData.content || [];
            state.pagination.totalPages = pageData.totalPages;
            state.pagination.totalElements = pageData.totalElements;

            renderEmployeesTable(state.employees);
            updatePaginationUI(pageData);
        }
    } catch (err) {
        console.error('Failed to load employees:', err);
        elements.employeesTableBody.innerHTML = `<tr><td colspan="7" class="empty-cell">Failed to load employee records.</td></tr>`;
    }
}

function renderEmployeesTable(employees) {
    if (employees.length === 0) {
        elements.employeesTableBody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-cell">
                    <p style="font-size: 1rem; color: #475569; margin-bottom: 0.5rem;">No employee records found</p>
                    <span style="font-size: 0.85rem;">Adjust your search or contact HR to link your employee ID.</span>
                </td>
            </tr>
        `;
        return;
    }

    const canEdit = canEditEmployees();
    const canDel = canDeleteEmployees();

    elements.employeesTableBody.innerHTML = employees.map(emp => {
        const initials = ((emp.firstName?.[0] || '') + (emp.lastName?.[0] || '')).toUpperCase();
        const statusClass = (emp.status || 'ACTIVE').toLowerCase();
        const formattedStatus = (emp.status || 'ACTIVE').replace('_', ' ');
        const formattedType = (emp.employmentType || 'FULL_TIME').replace('_', ' ');

        return `
            <tr>
                <td>
                    <div class="user-cell">
                        <div class="avatar">${initials}</div>
                        <div>
                            <div class="user-name">${escapeHtml(emp.fullName)} <span style="font-size: 0.75rem; color: #94a3b8; font-weight: normal;">(#${emp.id})</span></div>
                            <div class="user-email">${escapeHtml(emp.email)}</div>
                        </div>
                    </div>
                </td>
                <td>
                    <span class="badge-dept">${escapeHtml(emp.departmentName || 'General')}</span>
                </td>
                <td>${escapeHtml(emp.jobTitle)}</td>
                <td>
                    <span class="badge-type">${formattedType}</span>
                </td>
                <td>
                    <span class="salary-text">${formatCurrency(emp.salary)}</span>
                </td>
                <td>
                    <span class="badge badge-${statusClass}">${formattedStatus}</span>
                </td>
                <td class="text-right">
                    <div class="row-actions">
                        <button class="icon-btn" onclick="viewEmployeeProfile(${emp.id})" title="View Details">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                        </button>
                        ${canEdit ? `
                        <button class="icon-btn" onclick="editEmployee(${emp.id})" title="Edit Details (HR/Admin/Manager)">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                            </svg>
                        </button>` : ''}
                        ${canDel ? `
                        <button class="icon-btn danger" onclick="confirmDeleteEmployee(${emp.id}, '${escapeHtml(emp.fullName)}')" title="Delete Employee (Admin only)">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="3 6 5 6 21 6"></polyline>
                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                            </svg>
                        </button>` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function updatePaginationUI(pageData) {
    const total = pageData.totalElements;
    const size = pageData.size;
    const page = pageData.number;
    const totalPages = pageData.totalPages;

    const start = total === 0 ? 0 : page * size + 1;
    const end = Math.min((page + 1) * size, total);

    elements.paginationInfo.textContent = `Showing ${start}-${end} of ${total} record(s)`;
    elements.pageDisplay.textContent = `Page ${page + 1} of ${Math.max(totalPages, 1)}`;

    elements.btnPrevPage.disabled = page === 0;
    elements.btnNextPage.disabled = page >= totalPages - 1 || totalPages === 0;
}

// Modal Handlers
function openEmployeeModal(emp = null) {
    if (!canAddEmployees() && !emp) {
        showToast('Access Denied: Only HR and Admins can add new employees', 'error');
        return;
    }
    if (emp && !canEditEmployees()) {
        showToast('Access Denied: You do not have permission to edit records', 'error');
        return;
    }

    elements.formEmployee.reset();
    document.getElementById('emp-id').value = '';

    if (emp) {
        elements.modalEmployeeTitle.textContent = 'Edit Employee';
        document.getElementById('emp-id').value = emp.id;
        document.getElementById('emp-firstname').value = emp.firstName || '';
        document.getElementById('emp-lastname').value = emp.lastName || '';
        document.getElementById('emp-email').value = emp.email || '';
        document.getElementById('emp-phone').value = emp.phone || '';
        document.getElementById('emp-department').value = emp.departmentId || '';
        document.getElementById('emp-jobtitle').value = emp.jobTitle || '';
        document.getElementById('emp-salary').value = emp.salary || '';
        document.getElementById('emp-hiredate').value = emp.hireDate || '';
        document.getElementById('emp-type').value = emp.employmentType || 'FULL_TIME';
        document.getElementById('emp-status').value = emp.status || 'ACTIVE';
        document.getElementById('emp-address').value = emp.address || '';
    } else {
        elements.modalEmployeeTitle.textContent = 'Add New Employee (HR / Admin)';
        document.getElementById('emp-hiredate').valueAsDate = new Date();
    }

    elements.modalEmployee.classList.add('open');
}

function openDepartmentModal(dept = null) {
    if (!canManageDepartments()) {
        showToast('Access Denied: Only HR and Admins can manage departments', 'error');
        return;
    }
    elements.formDepartment.reset();
    document.getElementById('dept-id').value = '';

    if (dept) {
        elements.modalDeptTitle.textContent = 'Edit Department';
        document.getElementById('dept-id').value = dept.id;
        document.getElementById('dept-name').value = dept.name || '';
        document.getElementById('dept-code').value = dept.code || '';
        document.getElementById('dept-location').value = dept.location || '';
        document.getElementById('dept-description').value = dept.description || '';
    } else {
        elements.modalDeptTitle.textContent = 'Add Department (HR / Admin)';
    }

    elements.modalDept.classList.add('open');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('open');
}

// View Employee Profile
async function viewEmployeeProfile(id) {
    try {
        state.activeEmployeeId = id;
        const res = await authFetch(`${API_BASE}/employees/${id}`);
        const json = await res.json();
        if (json.success) {
            const emp = json.data;
            const initials = ((emp.firstName?.[0] || '') + (emp.lastName?.[0] || '')).toUpperCase();
            const formattedStatus = (emp.status || 'ACTIVE').replace('_', ' ');
            const formattedType = (emp.employmentType || 'FULL_TIME').replace('_', ' ');

            elements.viewEmployeeContent.innerHTML = `
                <div class="profile-view-header">
                    <div class="profile-avatar-lg">${initials}</div>
                    <div>
                        <h2 style="font-size: 1.35rem; font-weight: 700;">${escapeHtml(emp.fullName)}</h2>
                        <p style="color: #475569; font-size: 0.95rem;">${escapeHtml(emp.jobTitle)} • <span class="badge-dept">${escapeHtml(emp.departmentName)}</span></p>
                    </div>
                </div>
                <div class="profile-grid">
                    <div class="profile-field">
                        <label>Employee ID</label>
                        <div>#${emp.id}</div>
                    </div>
                    <div class="profile-field">
                        <label>Email Address</label>
                        <div>${escapeHtml(emp.email)}</div>
                    </div>
                    <div class="profile-field">
                        <label>Phone Number</label>
                        <div>${escapeHtml(emp.phone || 'N/A')}</div>
                    </div>
                    <div class="profile-field">
                        <label>Annual Compensation</label>
                        <div style="font-weight: 700; color: #10b981;">${formatCurrency(emp.salary)} / year</div>
                    </div>
                    <div class="profile-field">
                        <label>Hire Date</label>
                        <div>${emp.hireDate || 'N/A'}</div>
                    </div>
                    <div class="profile-field">
                        <label>Employment Type</label>
                        <div><span class="badge-type">${formattedType}</span></div>
                    </div>
                    <div class="profile-field">
                        <label>Current Status</label>
                        <div><span class="badge badge-${(emp.status || 'ACTIVE').toLowerCase()}">${formattedStatus}</span></div>
                    </div>
                    <div class="profile-field" style="grid-column: span 2;">
                        <label>Office / Residential Address</label>
                        <div>${escapeHtml(emp.address || 'No address provided')}</div>
                    </div>
                </div>
            `;
            elements.modalViewEmployee.classList.add('open');
        }
    } catch (err) {
        console.error('Error fetching employee details:', err);
    }
}

// Edit Trigger
function editEmployee(id) {
    if (!canEditEmployees()) {
        showToast('Access Denied: You do not have permission to edit records', 'error');
        return;
    }
    const emp = state.employees.find(e => e.id === id);
    if (emp) {
        openEmployeeModal(emp);
    } else {
        authFetch(`${API_BASE}/employees/${id}`)
            .then(r => r.json())
            .then(res => {
                if (res.success) openEmployeeModal(res.data);
            });
    }
}

// Delete Confirmation (Admin Only)
function confirmDeleteEmployee(id, name) {
    if (!canDeleteEmployees()) {
        showToast('Access Denied: Only Admins are authorized to delete employees', 'error');
        return;
    }
    state.activeDeleteTarget = { type: 'employee', id, name };
    elements.deleteMessage.innerHTML = `Are you sure you want to delete employee <strong>${escapeHtml(name)}</strong>?`;
    elements.modalDelete.classList.add('open');
}

function confirmDeleteDepartment(id, name) {
    if (!canDeleteDepartments()) {
        showToast('Access Denied: Only Admins can delete departments', 'error');
        return;
    }
    state.activeDeleteTarget = { type: 'department', id, name };
    elements.deleteMessage.innerHTML = `Are you sure you want to delete department <strong>${escapeHtml(name)}</strong>? Ensure no employees are assigned to it.`;
    elements.modalDelete.classList.add('open');
}

async function handleConfirmDelete() {
    if (!state.activeDeleteTarget) return;
    const { type, id, name } = state.activeDeleteTarget;

    try {
        const endpoint = type === 'employee' ? `${API_BASE}/employees/${id}` : `${API_BASE}/departments/${id}`;
        const res = await authFetch(endpoint, { method: 'DELETE' });
        const json = await res.json();

        if (res.ok && json.success) {
            showToast(`${type === 'employee' ? 'Employee' : 'Department'} "${name}" deleted successfully`, 'success');
            closeModal('modal-delete');
            if (type === 'employee') {
                loadEmployees();
                if (canViewDashboard()) loadDashboardStats();
            } else {
                loadDepartments();
                if (canViewDashboard()) loadDashboardStats();
            }
        } else {
            showToast(json.message || 'Deletion failed', 'error');
        }
    } catch (err) {
        console.error('Delete error:', err);
    }
}

// Save Employee Form
async function handleEmployeeFormSubmit(e) {
    e.preventDefault();

    const id = document.getElementById('emp-id').value;
    const payload = {
        firstName: document.getElementById('emp-firstname').value.trim(),
        lastName: document.getElementById('emp-lastname').value.trim(),
        email: document.getElementById('emp-email').value.trim(),
        phone: document.getElementById('emp-phone').value.trim(),
        departmentId: parseInt(document.getElementById('emp-department').value),
        jobTitle: document.getElementById('emp-jobtitle').value.trim(),
        salary: parseFloat(document.getElementById('emp-salary').value),
        hireDate: document.getElementById('emp-hiredate').value,
        employmentType: document.getElementById('emp-type').value,
        status: document.getElementById('emp-status').value,
        address: document.getElementById('emp-address').value.trim()
    };

    try {
        const isEdit = !!id;
        const url = isEdit ? `${API_BASE}/employees/${id}` : `${API_BASE}/employees`;
        const method = isEdit ? 'PUT' : 'POST';

        const res = await authFetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const json = await res.json();

        if (res.ok && json.success) {
            showToast(json.message || (isEdit ? 'Employee updated' : 'Employee created'), 'success');
            closeModal('modal-employee');
            loadEmployees();
            if (canViewDashboard()) loadDashboardStats();
        } else {
            const errorMsg = json.validationErrors ?
                Object.values(json.validationErrors).join(', ') :
                (json.message || 'Operation failed');
            showToast(errorMsg, 'error');
        }
    } catch (err) {
        console.error('Save error:', err);
    }
}

// Save Department Form
async function handleDepartmentFormSubmit(e) {
    e.preventDefault();

    const id = document.getElementById('dept-id').value;
    const payload = {
        name: document.getElementById('dept-name').value.trim(),
        code: document.getElementById('dept-code').value.trim().toUpperCase(),
        location: document.getElementById('dept-location').value.trim(),
        description: document.getElementById('dept-description').value.trim()
    };

    try {
        const isEdit = !!id;
        const url = isEdit ? `${API_BASE}/departments/${id}` : `${API_BASE}/departments`;
        const method = isEdit ? 'PUT' : 'POST';

        const res = await authFetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const json = await res.json();

        if (res.ok && json.success) {
            showToast(json.message || (isEdit ? 'Department updated' : 'Department created'), 'success');
            closeModal('modal-department');
            loadDepartments();
            if (canViewDashboard()) loadDashboardStats();
        } else {
            showToast(json.message || 'Failed to save department', 'error');
        }
    } catch (err) {
        console.error('Department save error:', err);
    }
}

// Utilities
function formatCurrency(num) {
    if (num === null || num === undefined) return '$0';
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 0
    }).format(num);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function showToast(message, type = 'info') {
    if (!elements.toastContainer) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span>${escapeHtml(message)}</span>`;

    elements.toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Global scope bindings for HTML onclick handlers
window.viewEmployeeProfile = viewEmployeeProfile;
window.editEmployee = editEmployee;
window.confirmDeleteEmployee = confirmDeleteEmployee;
window.confirmDeleteDepartment = confirmDeleteDepartment;
window.doLogin = doLogin;

// ===================================================================
// Leave Management Functions
// ===================================================================

function openApplyLeaveModal() {
    if (!elements.formApplyLeave) return;
    elements.formApplyLeave.reset();

    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const dayAfter = new Date(today);
    dayAfter.setDate(today.getDate() + 3);

    const minDateStr = today.toISOString().split('T')[0];
    const tomorrowStr = tomorrow.toISOString().split('T')[0];
    const dayAfterStr = dayAfter.toISOString().split('T')[0];

    if (elements.leaveStartDate) {
        elements.leaveStartDate.min = minDateStr;
        elements.leaveStartDate.value = tomorrowStr;
    }
    if (elements.leaveEndDate) {
        elements.leaveEndDate.min = minDateStr;
        elements.leaveEndDate.value = dayAfterStr;
    }

    updateDurationPreview();
    if (elements.modalApplyLeave) {
        elements.modalApplyLeave.classList.add('open');
    }
}

function updateDurationPreview() {
    if (!elements.leaveStartDate || !elements.leaveEndDate || !elements.leaveDurationPreview) return;
    const startVal = elements.leaveStartDate.value;
    const endVal = elements.leaveEndDate.value;

    if (!startVal || !endVal) {
        elements.leaveDurationPreview.style.display = 'none';
        return;
    }

    const start = new Date(startVal);
    const end = new Date(endVal);

    if (end < start) {
        elements.leaveDurationPreview.style.display = 'block';
        elements.leaveDurationPreview.style.borderColor = '#fca5a5';
        elements.leaveDurationPreview.style.backgroundColor = '#fef2f2';
        elements.leaveDurationPreview.style.color = '#b91c1c';
        elements.leaveDurationPreview.innerHTML = '⚠️ End date cannot be before start date';
        return;
    }

    const diffMs = end.getTime() - start.getTime();
    const days = Math.round(diffMs / (1000 * 60 * 60 * 24)) + 1;

    elements.leaveDurationPreview.style.display = 'flex';
    elements.leaveDurationPreview.style.borderColor = '#7dd3fc';
    elements.leaveDurationPreview.style.backgroundColor = '#f0f9ff';
    elements.leaveDurationPreview.style.color = '#0369a1';
    elements.leaveDurationPreview.innerHTML = `<span>📅 Calculated Duration:</span> <strong>${days}</strong> day${days > 1 ? 's' : ''}`;
}

async function handleApplyLeaveSubmit(e) {
    e.preventDefault();
    const leaveType = elements.leaveType.value;
    const startDate = elements.leaveStartDate.value;
    const endDate = elements.leaveEndDate.value;
    const reason = elements.leaveReason.value.trim();

    if (!startDate || !endDate) {
        showToast('Please select both start date and end date', 'error');
        return;
    }

    if (endDate < startDate) {
        showToast('End date cannot be earlier than start date', 'error');
        return;
    }

    if (!reason || reason.length < 5) {
        showToast('Reason must be at least 5 characters long', 'error');
        return;
    }

    try {
        const res = await authFetch(`${API_BASE}/leaves`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                leaveType,
                startDate,
                endDate,
                reason
            })
        });

        const json = await res.json();
        if (res.ok && json.success) {
            showToast('Leave application submitted successfully!', 'success');
            closeModal('modal-apply-leave');
            if (state.currentTab === 'leaves') {
                if (isEmployee()) {
                    loadMyLeaves();
                } else {
                    loadLeaveStats();
                    loadAllLeaves();
                }
            }
        } else {
            showToast(json.message || 'Failed to submit leave request', 'error');
        }
    } catch (err) {
        console.error('Leave application error:', err);
    }
}

async function loadLeaveStats() {
    if (isEmployee()) return;
    try {
        const res = await authFetch(`${API_BASE}/leaves/stats`);
        const json = await res.json();
        if (json.success && json.data) {
            const stats = json.data;
            if (elements.statLeavePending) elements.statLeavePending.textContent = stats.totalPending || 0;
            if (elements.statLeaveOnleave) elements.statLeaveOnleave.textContent = stats.currentlyOnLeave || 0;
            if (elements.statLeaveApproved) elements.statLeaveApproved.textContent = stats.totalApproved || 0;
            if (elements.statLeaveRejected) elements.statLeaveRejected.textContent = stats.totalRejected || 0;
        }
    } catch (err) {
        console.error('Failed to load leave stats:', err);
    }
}

async function loadAllLeaves() {
    try {
        const status = elements.filterLeaveStatus ? elements.filterLeaveStatus.value : '';
        const url = status ? `${API_BASE}/leaves?status=${status}` : `${API_BASE}/leaves`;
        const res = await authFetch(url);
        const json = await res.json();
        if (json.success) {
            renderLeavesTable(json.data || [], false);
        }
    } catch (err) {
        console.error('Failed to load company leaves:', err);
    }
}

async function loadMyLeaves() {
    try {
        const res = await authFetch(`${API_BASE}/leaves/my-leaves`);
        const json = await res.json();
        if (json.success) {
            renderLeavesTable(json.data || [], true);
        }
    } catch (err) {
        console.error('Failed to load my leaves:', err);
    }
}

function renderLeavesTable(leaves, isPersonalView) {
    if (!elements.leavesTableBody) return;

    if (!leaves || leaves.length === 0) {
        elements.leavesTableBody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center" style="padding: 2.5rem; color: #94a3b8;">
                    ${isPersonalView ? 'You have not submitted any leave applications yet.' : 'No leave applications found.'}
                </td>
            </tr>
        `;
        return;
    }

    elements.leavesTableBody.innerHTML = leaves.map(l => {
        const empName = escapeHtml(l.employeeName || 'Staff Member');
        const dept = escapeHtml(l.departmentName || 'General');
        const type = escapeHtml(l.leaveType || 'CASUAL');
        const status = l.status || 'PENDING';
        const reason = escapeHtml(l.reason || '');
        const duration = l.durationDays || 1;
        const dateRange = `${l.startDate} &rarr; ${l.endDate}`;
        const reviewerInfo = l.reviewedByUsername
            ? `${escapeHtml(l.reviewedByUsername)}: ${escapeHtml(l.reviewComments || 'Approved')}`
            : (l.status === 'PENDING' ? '<span style="color:#94a3b8; font-style:italic;">Pending Review</span>' : '-');

        let actionsHtml = '-';
        if (isPersonalView) {
            if (status === 'PENDING') {
                actionsHtml = `
                    <button class="btn-action-cancel" onclick="cancelLeave(${l.id})" title="Cancel this pending request">
                        Cancel Request
                    </button>
                `;
            }
        } else {
            if (status === 'PENDING') {
                actionsHtml = `
                    <div class="btn-group-actions">
                        <button class="btn-action-approve" onclick="approveLeave(${l.id})" title="Approve Leave">
                            ✓ Approve
                        </button>
                        <button class="btn-action-reject" onclick="openRejectModal(${l.id})" title="Reject Leave">
                            ✕ Reject
                        </button>
                    </div>
                `;
            }
        }

        return `
            <tr>
                <td>
                    <div class="user-cell">
                        <div class="user-avatar-sm" style="width: 32px; height: 32px; font-size: 0.75rem;">
                            ${escapeHtml((empName[0] || 'E').toUpperCase())}
                        </div>
                        <div>
                            <strong>${empName}</strong>
                            <div style="font-size: 0.75rem; color: #64748b;">${dept}</div>
                        </div>
                    </div>
                </td>
                <td><span class="badge-leave-type">${type.toLowerCase()}</span></td>
                <td style="font-size: 0.85rem; font-weight: 500;">${dateRange}</td>
                <td><strong>${duration}</strong> day${duration > 1 ? 's' : ''}</td>
                <td style="max-width: 220px; font-size: 0.85rem;" title="${reason}">${reason}</td>
                <td><span class="badge badge-${status.toLowerCase()}">${status}</span></td>
                <td style="font-size: 0.8rem; color: #475569; max-width: 200px;">${reviewerInfo}</td>
                <td style="text-align: right;">${actionsHtml}</td>
            </tr>
        `;
    }).join('');
}

async function approveLeave(id) {
    if (!confirm('Are you sure you want to approve this leave request?')) return;
    try {
        const res = await authFetch(`${API_BASE}/leaves/${id}/approve`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reviewComments: 'Approved' })
        });
        const json = await res.json();
        if (res.ok && json.success) {
            showToast('Leave request approved successfully!', 'success');
            loadLeaveStats();
            loadAllLeaves();
            if (canViewDashboard()) loadDashboardStats();
        } else {
            showToast(json.message || 'Failed to approve leave', 'error');
        }
    } catch (err) {
        console.error('Approve leave error:', err);
    }
}

function openRejectModal(id) {
    if (!elements.modalRejectLeave) return;
    if (elements.rejectLeaveId) elements.rejectLeaveId.value = id;
    if (elements.rejectComments) elements.rejectComments.value = '';
    elements.modalRejectLeave.classList.add('open');
}

async function handleRejectLeaveSubmit(e) {
    e.preventDefault();
    const id = elements.rejectLeaveId.value;
    const reviewComments = elements.rejectComments.value.trim() || 'Rejected';

    try {
        const res = await authFetch(`${API_BASE}/leaves/${id}/reject`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reviewComments })
        });
        const json = await res.json();
        if (res.ok && json.success) {
            showToast('Leave request rejected', 'info');
            closeModal('modal-reject-leave');
            loadLeaveStats();
            loadAllLeaves();
        } else {
            showToast(json.message || 'Failed to reject leave', 'error');
        }
    } catch (err) {
        console.error('Reject leave error:', err);
    }
}

async function cancelLeave(id) {
    if (!confirm('Are you sure you want to cancel this pending leave request?')) return;
    try {
        const res = await authFetch(`${API_BASE}/leaves/${id}/cancel`, {
            method: 'PATCH'
        });
        const json = await res.json();
        if (res.ok && json.success) {
            showToast('Leave request cancelled successfully', 'info');
            if (isEmployee()) {
                loadMyLeaves();
            } else {
                loadLeaveStats();
                loadAllLeaves();
            }
        } else {
            showToast(json.message || 'Failed to cancel leave request', 'error');
        }
    } catch (err) {
        console.error('Cancel leave error:', err);
    }
}

window.openApplyLeaveModal = openApplyLeaveModal;
window.approveLeave = approveLeave;
window.openRejectModal = openRejectModal;
window.cancelLeave = cancelLeave;
