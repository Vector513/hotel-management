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
  MenuItem,
  Typography,
  Chip,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { schedulesAPI, employeesAPI } from '../../services/api';
import type { CleaningSchedule, CreateCleaningScheduleRequest, UpdateCleaningScheduleRequest, Employee } from '../../types';
import { DayOfWeek } from '../../types';

const dayOfWeekLabels: Record<DayOfWeek, string> = {
  [DayOfWeek.MONDAY]: 'Понедельник',
  [DayOfWeek.TUESDAY]: 'Вторник',
  [DayOfWeek.WEDNESDAY]: 'Среда',
  [DayOfWeek.THURSDAY]: 'Четверг',
  [DayOfWeek.FRIDAY]: 'Пятница',
  [DayOfWeek.SATURDAY]: 'Суббота',
  [DayOfWeek.SUNDAY]: 'Воскресенье',
};

const SchedulesManagement: React.FC = () => {
  const [schedules, setSchedules] = useState<CleaningSchedule[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<CleaningSchedule | null>(null);
  const [formData, setFormData] = useState<CreateCleaningScheduleRequest>({
    employeeId: 0,
    floor: 1,
    dayOfWeek: DayOfWeek.MONDAY,
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [schedulesData, employeesData] = await Promise.all([
        schedulesAPI.getAll(),
        employeesAPI.getAll(),
      ]);
      setSchedules(schedulesData);
      setEmployees(employeesData);
      if (employeesData.length > 0 && formData.employeeId === 0) {
        setFormData({ ...formData, employeeId: employeesData[0].employeeId });
      }
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки данных');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (schedule?: CleaningSchedule) => {
    if (schedule) {
      setEditingSchedule(schedule);
      setFormData({
        employeeId: schedule.employeeId,
        floor: schedule.floor,
        dayOfWeek: schedule.dayOfWeek,
      });
    } else {
      setEditingSchedule(null);
      setFormData({
        employeeId: employees[0]?.employeeId || 0,
        floor: 1,
        dayOfWeek: DayOfWeek.MONDAY,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingSchedule(null);
  };

  const handleSubmit = async () => {
    try {
      if (editingSchedule) {
        await schedulesAPI.update(editingSchedule.scheduleId, formData as UpdateCleaningScheduleRequest);
      } else {
        await schedulesAPI.create(formData);
      }
      await loadData();
      handleCloseDialog();
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка сохранения');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить эту запись расписания?')) return;
    try {
      await schedulesAPI.delete(id);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка удаления');
    }
  };

  if (loading && schedules.length === 0) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление расписанием уборки</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Добавить запись
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
              <TableCell>Сотрудник</TableCell>
              <TableCell>Этаж</TableCell>
              <TableCell>День недели</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {schedules.map((schedule) => {
              const employee = employees.find((e) => e.employeeId === schedule.employeeId);
              return (
                <TableRow key={schedule.scheduleId}>
                  <TableCell>{schedule.scheduleId}</TableCell>
                  <TableCell>{employee?.fullName || schedule.employeeId}</TableCell>
                  <TableCell>{schedule.floor}</TableCell>
                  <TableCell>
                    <Chip
                      label={dayOfWeekLabels[schedule.dayOfWeek] || schedule.dayOfWeek}
                      color="primary"
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => handleOpenDialog(schedule)}>
                      <EditIcon />
                    </IconButton>
                    <IconButton size="small" onClick={() => handleDelete(schedule.scheduleId)}>
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingSchedule ? 'Редактировать расписание' : 'Добавить расписание'}</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            select
            label="Сотрудник"
            value={formData.employeeId}
            onChange={(e) => setFormData({ ...formData, employeeId: parseInt(e.target.value) })}
            margin="normal"
            required
          >
            {employees.map((employee) => (
              <MenuItem key={employee.employeeId} value={employee.employeeId}>
                {employee.fullName} (Этаж {employee.floor})
              </MenuItem>
            ))}
          </TextField>
          <TextField
            fullWidth
            label="Этаж"
            type="number"
            value={formData.floor}
            onChange={(e) => setFormData({ ...formData, floor: parseInt(e.target.value) || 1 })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            select
            label="День недели"
            value={formData.dayOfWeek}
            onChange={(e) => setFormData({ ...formData, dayOfWeek: e.target.value as DayOfWeek })}
            margin="normal"
            required
          >
            {Object.values(DayOfWeek).map((day) => (
              <MenuItem key={day} value={day}>
                {dayOfWeekLabels[day]}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Отмена</Button>
          <Button onClick={handleSubmit} variant="contained">
            {editingSchedule ? 'Сохранить' : 'Создать'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulesManagement;

