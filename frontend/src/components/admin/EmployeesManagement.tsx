import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Alert,
  CircularProgress,
  Typography,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { employeesAPI } from '../../services/api';
import type { Employee, CreateEmployeeRequest, UpdateEmployeeRequest } from '../../types';

const EmployeesManagement: React.FC = () => {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null);
  const [formData, setFormData] = useState<CreateEmployeeRequest>({
    fullName: '',
    floor: 1,
  });
  const [showCredentials, setShowCredentials] = useState(false);
  const [credentials, setCredentials] = useState<{ login: string; password: string } | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const data = await employeesAPI.getAll();
      setEmployees(data);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки данных');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (employee?: Employee) => {
    if (employee) {
      setEditingEmployee(employee);
      setFormData({
        fullName: employee.fullName,
        floor: employee.floor,
      });
    } else {
      setEditingEmployee(null);
      setFormData({
        fullName: '',
        floor: 1,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingEmployee(null);
    setCredentials(null);
    setShowCredentials(false);
    // Сбрасываем форму
    setFormData({
      fullName: '',
      floor: 1,
    });
  };

  const handleSubmit = async () => {
    try {
      if (editingEmployee) {
        await employeesAPI.update(editingEmployee.employeeId, formData as UpdateEmployeeRequest);
        await loadData();
        handleCloseDialog();
      } else {
        const response = await employeesAPI.create(formData);
        setCredentials({ login: response.login, password: response.password });
        setShowCredentials(true);
        await loadData();
        // Не закрываем диалог, чтобы пользователь увидел логин и пароль
      }
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка сохранения');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить этого сотрудника?')) return;
    try {
      await employeesAPI.delete(id);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка удаления');
    }
  };

  if (loading && employees.length === 0) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление сотрудниками</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Добавить сотрудника
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>ФИО</TableCell>
              <TableCell>Этаж</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {employees.map((employee) => (
              <TableRow key={employee.employeeId}>
                <TableCell>{employee.employeeId}</TableCell>
                <TableCell>{employee.fullName}</TableCell>
                <TableCell>{employee.floor}</TableCell>
                <TableCell>
                  <IconButton size="small" onClick={() => handleOpenDialog(employee)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDelete(employee.employeeId)}>
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingEmployee ? 'Редактировать сотрудника' : 'Добавить сотрудника'}</DialogTitle>
        <DialogContent>
          {showCredentials && credentials && (
            <Alert severity="success" sx={{ mb: 2 }}>
              <Typography variant="subtitle2" gutterBottom>
                Сотрудник создан!
              </Typography>
              <Typography variant="body2">
                <strong>Логин:</strong> {credentials.login}
              </Typography>
              <Typography variant="body2">
                <strong>Пароль:</strong> {credentials.password}
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                Сохраните эти данные! Пароль больше не будет показан.
              </Typography>
            </Alert>
          )}
          <TextField
            fullWidth
            label="ФИО"
            value={formData.fullName}
            onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Этаж"
            type="number"
            value={formData.floor}
            onChange={(e) => setFormData({ ...formData, floor: parseInt(e.target.value) || 1 })}
            margin="normal"
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>{showCredentials ? 'Закрыть' : 'Отмена'}</Button>
          {!showCredentials && (
            <Button onClick={handleSubmit} variant="contained">
              {editingEmployee ? 'Сохранить' : 'Создать'}
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default EmployeesManagement;

