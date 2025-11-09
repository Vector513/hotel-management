import axios from 'axios';
import type {
  Client,
  CreateClientRequest,
  UpdateClientRequest,
  ClientCreatedResponse,
  Room,
  CreateRoomRequest,
  UpdateRoomRequest,
  FreeRoomsResponse,
  Invoice,
  CreateInvoiceRequest,
  UpdateInvoiceRequest,
  InvoiceResponse,
  Employee,
  CreateEmployeeRequest,
  UpdateEmployeeRequest,
  EmployeeCreatedResponse,
  CleaningSchedule,
  CreateCleaningScheduleRequest,
  UpdateCleaningScheduleRequest,
  LoginRequest,
  LoginResponse,
  User,
  QuarterlyReport
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor для добавления токена
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Interceptor для обработки ошибок
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // Не редиректим на /login если мы уже на странице логина
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    
    // Логируем ошибку для отладки
    if (error.response) {
      console.error('API Error:', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data,
        url: error.config?.url
      });
    } else if (error.request) {
      console.error('Network Error:', error.request);
    } else {
      console.error('Error:', error.message);
    }
    
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/login', data);
    return response.data;
  },
  getClientCleaner: async (clientId: number, dayOfWeek: string): Promise<{ employeeName: string }> => {
    const response = await api.get<{ employeeName: string }>(`/clients/${clientId}/cleaner?dayOfWeek=${encodeURIComponent(dayOfWeek)}`);
    return response.data;
  },
  getRoomCleaner: async (roomId: number, dayOfWeek: string): Promise<{ employeeName: string }> => {
    const response = await api.get<{ employeeName: string }>(`/rooms/${roomId}/cleaner?dayOfWeek=${encodeURIComponent(dayOfWeek)}`);
    return response.data;
  },
};

// Admin API - Clients
export const clientsAPI = {
  getAll: async (): Promise<Client[]> => {
    const response = await api.get<Client[]>('/admin/clients');
    return response.data;
  },
  getById: async (id: number): Promise<Client> => {
    const response = await api.get<Client>(`/admin/clients/${id}`);
    return response.data;
  },
  getByCity: async (city: string): Promise<Client[]> => {
    const response = await api.get<Client[]>(`/admin/clients/from?city=${encodeURIComponent(city)}`);
    return response.data;
  },
  getCleaner: async (clientId: number, dayOfWeek: string): Promise<{ employeeName: string }> => {
    const response = await api.get<{ employeeName: string }>(`/admin/clients/${clientId}/cleaner?dayOfWeek=${dayOfWeek}`);
    return response.data;
  },
  create: async (data: CreateClientRequest): Promise<ClientCreatedResponse> => {
    const response = await api.post<ClientCreatedResponse>('/admin/clients', data);
    return response.data;
  },
  update: async (id: number, data: UpdateClientRequest): Promise<void> => {
    await api.put(`/admin/clients/${id}`, data);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/admin/clients/${id}`);
  },
};

// Admin API - Rooms
export const roomsAPI = {
  getAll: async (): Promise<Room[]> => {
    const response = await api.get<Room[]>('/admin/rooms');
    return response.data;
  },
  getById: async (id: number): Promise<Room> => {
    const response = await api.get<Room>(`/admin/rooms/${id}`);
    return response.data;
  },
  getFree: async (): Promise<FreeRoomsResponse> => {
    const response = await api.get<FreeRoomsResponse>('/admin/rooms/free');
    return response.data;
  },
  getResidents: async (roomId: number): Promise<Client[]> => {
    const response = await api.get<Client[]>(`/admin/rooms/${roomId}/residents`);
    return response.data;
  },
  create: async (data: CreateRoomRequest): Promise<{ roomId: number }> => {
    const response = await api.post<{ roomId: number }>('/admin/rooms', data);
    return response.data;
  },
  update: async (id: number, data: UpdateRoomRequest): Promise<void> => {
    await api.put(`/admin/rooms/${id}`, data);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/admin/rooms/${id}`);
  },
};

// Admin API - Invoices
export const invoicesAPI = {
  getAll: async (): Promise<Invoice[]> => {
    const response = await api.get<Invoice[]>('/admin/invoices');
    return response.data;
  },
  getById: async (id: number): Promise<Invoice> => {
    const response = await api.get<Invoice>(`/admin/invoices/${id}`);
    return response.data;
  },
  create: async (data: CreateInvoiceRequest): Promise<InvoiceResponse> => {
    const response = await api.post<InvoiceResponse>('/admin/invoices', data);
    return response.data;
  },
  createForClient: async (clientId: number): Promise<InvoiceResponse> => {
    const response = await api.post<InvoiceResponse>(`/admin/clients/${clientId}/invoice`);
    return response.data;
  },
  update: async (id: number, data: UpdateInvoiceRequest): Promise<void> => {
    await api.put(`/admin/invoices/${id}`, data);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/admin/invoices/${id}`);
  },
};

// Admin API - Employees
export const employeesAPI = {
  getAll: async (): Promise<Employee[]> => {
    const response = await api.get<Employee[]>('/admin/employees');
    return response.data;
  },
  getById: async (id: number): Promise<Employee> => {
    const response = await api.get<Employee>(`/admin/employees/${id}`);
    return response.data;
  },
  create: async (data: CreateEmployeeRequest): Promise<EmployeeCreatedResponse> => {
    const response = await api.post<EmployeeCreatedResponse>('/admin/employees', data);
    return response.data;
  },
  update: async (id: number, data: UpdateEmployeeRequest): Promise<void> => {
    await api.put(`/admin/employees/${id}`, data);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/admin/employees/${id}`);
  },
};

// Admin API - Cleaning Schedules
export const schedulesAPI = {
  getAll: async (): Promise<CleaningSchedule[]> => {
    const response = await api.get<CleaningSchedule[]>('/admin/schedules');
    return response.data;
  },
  getById: async (id: number): Promise<CleaningSchedule> => {
    const response = await api.get<CleaningSchedule>(`/admin/schedules/${id}`);
    return response.data;
  },
  create: async (data: CreateCleaningScheduleRequest): Promise<{ scheduleId: number }> => {
    const response = await api.post<{ scheduleId: number }>('/admin/schedules', data);
    return response.data;
  },
  update: async (id: number, data: UpdateCleaningScheduleRequest): Promise<void> => {
    await api.put(`/admin/schedules/${id}`, data);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/admin/schedules/${id}`);
  },
};

// Admin API - Users
export const usersAPI = {
  getAll: async (): Promise<User[]> => {
    const response = await api.get<User[]>('/admin/users');
    return response.data;
  },
  getById: async (id: number): Promise<User> => {
    const response = await api.get<User>(`/admin/users/${id}`);
    return response.data;
  },
  create: async (data: { username: string; password: string; role: string }): Promise<{ userId: number }> => {
    const response = await api.post<{ userId: number }>('/admin/users', data);
    return response.data;
  },
  update: async (id: number, data: { username: string; password: string; role: string }): Promise<void> => {
    await api.put(`/admin/users/${id}`, data);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/admin/users/${id}`);
  },
};

// Admin API - Reports
export const reportsAPI = {
  getQuarterlyReport: async (testDate?: string): Promise<QuarterlyReport> => {
    const url = testDate 
      ? `/admin/reports/quarterly?testDate=${encodeURIComponent(testDate)}`
      : '/admin/reports/quarterly';
    const response = await api.get<QuarterlyReport>(url);
    return response.data;
  },
};

// Client API
export const clientInvoicesAPI = {
  getMyInvoices: async (): Promise<Invoice[]> => {
    const response = await api.get<Invoice[]>('/client/invoices');
    return response.data;
  },
  requestInvoice: async (): Promise<InvoiceResponse> => {
    const response = await api.post<InvoiceResponse>('/client/requestInvoice');
    return response.data;
  },
};

// Employee API
export const employeeScheduleAPI = {
  getMySchedule: async (): Promise<CleaningSchedule[]> => {
    const response = await api.get<CleaningSchedule[]>('/employee/mySchedule');
    return response.data;
  },
};

