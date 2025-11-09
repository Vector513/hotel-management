export enum UserRole {
  ADMIN = 'ADMIN',
  WORKER = 'WORKER',
  CLIENT = 'CLIENT'
}

export enum RoomType {
  SINGLE = 'SINGLE',
  DOUBLE = 'DOUBLE',
  TRIPLE = 'TRIPLE'
}

export enum DayOfWeek {
  MONDAY = 'MONDAY',
  TUESDAY = 'TUESDAY',
  WEDNESDAY = 'WEDNESDAY',
  THURSDAY = 'THURSDAY',
  FRIDAY = 'FRIDAY',
  SATURDAY = 'SATURDAY',
  SUNDAY = 'SUNDAY'
}

export interface User {
  id: number;
  username: string;
  role: UserRole;
  clientId?: number;
  employeeId?: number;
  fullName?: string;
}

export interface Client {
  clientId: number;
  passportNumber: string;
  fullName: string;
  city: string;
  checkInDate: string;
  daysReserved: number;
  roomId: number;
  isResident: boolean;
}

export interface CreateClientRequest {
  passportNumber: string;
  fullName: string;
  city: string;
  checkInDate: string;
  daysReserved: number;
  roomId: number;
}

export interface UpdateClientRequest {
  passportNumber: string;
  fullName: string;
  city: string;
  checkInDate: string;
  daysReserved: number;
  roomId: number;
  isResident?: boolean;
}

export interface ClientCreatedResponse {
  clientId: number;
  login: string;
  password: string;
}

export interface Room {
  roomId: number;
  roomNumber: number;
  floor: number;
  type: RoomType;
  pricePerDay: string;
  phoneNumber: string;
}

export interface CreateRoomRequest {
  roomNumber: number;
  floor: number;
  type: RoomType;
  pricePerDay: string;
  phoneNumber: string;
}

export interface UpdateRoomRequest {
  roomNumber: number;
  floor: number;
  type: RoomType;
  pricePerDay: string;
  phoneNumber: string;
}

export interface FreeRoomsResponse {
  totalRooms: number;
  freeRoomsCount: number;
  freeRooms: Room[];
}

export interface Invoice {
  invoiceId: number;
  clientId: number;
  totalAmount: string;
  issueDate: string;
}

export interface CreateInvoiceRequest {
  clientId: number;
  issueDate?: string; // Опционально, если не указана, используется сегодняшняя дата
}

export interface UpdateInvoiceRequest {
  clientId: number;
  totalAmount: string;
  issueDate: string;
}

export interface InvoiceResponse {
  invoiceId: number;
  amount: string;
}

export interface Employee {
  employeeId: number;
  fullName: string;
  floor: number;
}

export interface CreateEmployeeRequest {
  fullName: string;
  floor: number;
}

export interface UpdateEmployeeRequest {
  fullName: string;
  floor: number;
}

export interface EmployeeCreatedResponse {
  employeeId: number;
  login: string;
  password: string;
}

export interface CleaningSchedule {
  scheduleId: number;
  employeeId: number;
  floor: number;
  dayOfWeek: DayOfWeek;
}

export interface CreateCleaningScheduleRequest {
  employeeId: number;
  floor: number;
  dayOfWeek: DayOfWeek;
}

export interface UpdateCleaningScheduleRequest {
  employeeId: number;
  floor: number;
  dayOfWeek: DayOfWeek;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface RoomOccupancyInfo {
  roomId: number;
  roomNumber: number;
  floor: number;
  type: RoomType;
  occupiedDays: number;
  freeDays: number;
  totalDays: number;
}

export interface QuarterlyReport {
  periodStart: string;
  periodEnd: string;
  totalClients: number;
  totalRevenue: string;
  roomOccupancy: RoomOccupancyInfo[];
}

