import React, { useState, useEffect } from 'react';
import {
  Container,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Box,
  CircularProgress,
  Alert,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
} from '@mui/material';
import CleaningServicesIcon from '@mui/icons-material/CleaningServices';
import { employeeScheduleAPI, authAPI } from '../services/api';
import type { CleaningSchedule } from '../types';
import { DayOfWeek } from '../types';

const dayOfWeekLabels: Record<DayOfWeek, string> = {
  [DayOfWeek.MONDAY]: 'Понедельник',
  [DayOfWeek.TUESDAY]: 'Вторник',
  [DayOfWeek.WEDNESDAY]: 'Среда',
  [DayOfWeek.THURSDAY]: 'Четверг',
  [DayOfWeek.FRIDAY]: 'Пятница',
  [DayOfWeek.SATURDAY]: 'Суббота',
  [DayOfWeek.SUNDAY]: 'Воскресенье',
};

const EmployeePanel: React.FC = () => {
  const [schedules, setSchedules] = useState<CleaningSchedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openCleanerDialog, setOpenCleanerDialog] = useState(false);
  const [clientId, setClientId] = useState<string>('');
  const [selectedDayOfWeek, setSelectedDayOfWeek] = useState<DayOfWeek>(DayOfWeek.MONDAY);
  const [cleanerName, setCleanerName] = useState<string>('');
  const [loadingCleaner, setLoadingCleaner] = useState(false);
  const [cleanerError, setCleanerError] = useState('');

  useEffect(() => {
    loadSchedule();
  }, []);

  const loadSchedule = async () => {
    try {
      setLoading(true);
      const data = await employeeScheduleAPI.getMySchedule();
      setSchedules(data);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки расписания');
    } finally {
      setLoading(false);
    }
  };

  const handleGetCleaner = async () => {
    const clientIdNum = parseInt(clientId);
    if (!clientIdNum || isNaN(clientIdNum)) {
      setCleanerError('Введите корректный ID клиента');
      return;
    }

    try {
      setLoadingCleaner(true);
      setCleanerError('');
      const response = await authAPI.getClientCleaner(clientIdNum, selectedDayOfWeek);
      setCleanerName(response.employeeName);
    } catch (err: any) {
      setCleanerError(err.response?.data || 'Ошибка получения информации об уборщике');
      setCleanerName('');
    } finally {
      setLoadingCleaner(false);
    }
  };

  if (loading) {
    return (
      <Container maxWidth="md">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="md">
      <Box display="flex" justifyContent="space-between" alignItems="center" sx={{ mt: 3, mb: 2 }}>
        <Typography variant="h4" component="h1">
          Мое расписание уборки
        </Typography>
        <Button
          variant="outlined"
          startIcon={<CleaningServicesIcon />}
          onClick={() => setOpenCleanerDialog(true)}
        >
          Узнать уборщика клиента
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>День недели</TableCell>
              <TableCell align="right">Этаж</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {schedules.length === 0 ? (
              <TableRow>
                <TableCell colSpan={2} align="center">
                  Нет записей в расписании
                </TableCell>
              </TableRow>
            ) : (
              schedules.map((schedule) => (
                <TableRow key={schedule.scheduleId}>
                  <TableCell>
                    <Chip
                      label={dayOfWeekLabels[schedule.dayOfWeek] || schedule.dayOfWeek}
                      color="primary"
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell align="right">{schedule.floor}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог для получения информации об уборщике */}
      <Dialog open={openCleanerDialog} onClose={() => setOpenCleanerDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Узнать уборщика номера клиента</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="ID клиента"
            type="number"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            margin="normal"
            required
            helperText="Введите ID клиента, для которого нужно узнать уборщика"
          />
          <TextField
            fullWidth
            select
            label="День недели"
            value={selectedDayOfWeek}
            onChange={(e) => setSelectedDayOfWeek(e.target.value as DayOfWeek)}
            margin="normal"
            required
          >
            {Object.values(DayOfWeek).map((day) => (
              <MenuItem key={day} value={day}>
                {dayOfWeekLabels[day]}
              </MenuItem>
            ))}
          </TextField>
          <Button
            fullWidth
            variant="contained"
            onClick={handleGetCleaner}
            disabled={loadingCleaner}
            sx={{ mt: 2 }}
          >
            {loadingCleaner ? <CircularProgress size={24} /> : 'Узнать уборщика'}
          </Button>
          {cleanerError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {cleanerError}
            </Alert>
          )}
          {cleanerName && (
            <Alert severity="success" sx={{ mt: 2 }}>
              <Typography variant="subtitle2" gutterBottom>
                Уборщик номера:
              </Typography>
              <Typography variant="h6">
                {cleanerName}
              </Typography>
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setOpenCleanerDialog(false);
            setClientId('');
            setCleanerName('');
            setCleanerError('');
          }}>
            Закрыть
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default EmployeePanel;

