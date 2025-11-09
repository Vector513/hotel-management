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
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography
          variant="h4"
          component="h1"
          sx={{
            fontWeight: 700,
            mb: 0.5,
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}
        >
          Мое расписание уборки
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Расписание работы и управление уборкой
        </Typography>
      </Box>

      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        flexWrap="wrap"
        gap={2}
        sx={{ mb: 3 }}
      >
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 600, mb: 0.5 }}>
            Записей в расписании: {schedules.length}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Этажи: {[...new Set(schedules.map(s => s.floor))].join(', ') || 'Нет'}
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<CleaningServicesIcon />}
          onClick={() => setOpenCleanerDialog(true)}
          sx={{
            borderColor: 'primary.main',
            '&:hover': {
              borderColor: 'primary.dark',
              bgcolor: 'primary.light',
              color: 'white',
            },
          }}
        >
          Узнать уборщика клиента
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>
          {error}
        </Alert>
      )}

      <TableContainer
        component={Paper}
        elevation={0}
        sx={{
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 2,
          overflow: 'hidden',
        }}
      >
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 600 }}>День недели</TableCell>
              <TableCell align="right" sx={{ fontWeight: 600 }}>Этаж</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {schedules.length === 0 ? (
              <TableRow>
                <TableCell colSpan={2} align="center" sx={{ py: 6 }}>
                  <Box sx={{ textAlign: 'center' }}>
                    <CleaningServicesIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2, opacity: 0.5 }} />
                    <Typography variant="body1" color="text.secondary" sx={{ fontWeight: 500 }}>
                      Нет записей в расписании
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Обратитесь к администратору для добавления расписания
                    </Typography>
                  </Box>
                </TableCell>
              </TableRow>
            ) : (
              schedules.map((schedule) => (
                <TableRow
                  key={schedule.scheduleId}
                  sx={{
                    '&:hover': {
                      bgcolor: 'action.hover',
                    },
                    '&:last-child td': {
                      borderBottom: 0,
                    },
                  }}
                >
                  <TableCell>
                    <Chip
                      label={dayOfWeekLabels[schedule.dayOfWeek] || schedule.dayOfWeek}
                      color="primary"
                      variant="outlined"
                      sx={{ fontWeight: 500 }}
                    />
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600, fontSize: '1.1rem' }}>
                    {schedule.floor} этаж
                  </TableCell>
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
    </Box>
  );
};

export default EmployeePanel;

