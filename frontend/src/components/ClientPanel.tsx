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
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
} from '@mui/material';
import CleaningServicesIcon from '@mui/icons-material/CleaningServices';
import ReceiptIcon from '@mui/icons-material/Receipt';
import { clientInvoicesAPI, authAPI } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import type { Invoice } from '../types';
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

const ClientPanel: React.FC = () => {
  const { user } = useAuth();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openCleanerDialog, setOpenCleanerDialog] = useState(false);
  const [selectedDayOfWeek, setSelectedDayOfWeek] = useState<DayOfWeek>(DayOfWeek.MONDAY);
  const [cleanerName, setCleanerName] = useState<string>('');
  const [loadingCleaner, setLoadingCleaner] = useState(false);
  const [cleanerError, setCleanerError] = useState('');
  const [requestingInvoice, setRequestingInvoice] = useState(false);
  const [invoiceRequestError, setInvoiceRequestError] = useState('');
  const [invoiceRequestSuccess, setInvoiceRequestSuccess] = useState(false);

  useEffect(() => {
    loadInvoices();
  }, []);

  const loadInvoices = async () => {
    try {
      setLoading(true);
      const data = await clientInvoicesAPI.getMyInvoices();
      setInvoices(data);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки счетов');
    } finally {
      setLoading(false);
    }
  };

  const handleGetCleaner = async () => {
    if (!user?.clientId) {
      setCleanerError('Не удалось определить ID клиента');
      return;
    }

    try {
      setLoadingCleaner(true);
      setCleanerError('');
      const response = await authAPI.getClientCleaner(user.clientId, selectedDayOfWeek);
      setCleanerName(response.employeeName);
    } catch (err: any) {
      setCleanerError(err.response?.data || 'Ошибка получения информации об уборщике');
      setCleanerName('');
    } finally {
      setLoadingCleaner(false);
    }
  };

  const handleRequestInvoice = async () => {
    try {
      setRequestingInvoice(true);
      setInvoiceRequestError('');
      setInvoiceRequestSuccess(false);
      await clientInvoicesAPI.requestInvoice();
      setInvoiceRequestSuccess(true);
      await loadInvoices(); // Обновляем список счетов
    } catch (err: any) {
      setInvoiceRequestError(err.response?.data || 'Ошибка запроса счета');
    } finally {
      setRequestingInvoice(false);
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
          Мои счета
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Просмотр и управление вашими счетами
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
            Всего счетов: {invoices.length}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Общая сумма: {invoices.reduce((sum, inv) => sum + parseFloat(inv.totalAmount), 0).toLocaleString('ru-RU')} ₽
          </Typography>
        </Box>
        <Box display="flex" gap={1.5} flexWrap="wrap">
          <Button
            variant="contained"
            startIcon={<ReceiptIcon />}
            onClick={handleRequestInvoice}
            disabled={requestingInvoice}
            sx={{
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              '&:hover': {
                background: 'linear-gradient(135deg, #5568d3 0%, #6a3d8f 100%)',
                boxShadow: '0 10px 25px rgba(102, 126, 234, 0.4)',
              },
            }}
          >
            {requestingInvoice ? 'Запрос...' : 'Запросить счет'}
          </Button>
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
            Узнать уборщика
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {invoiceRequestError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setInvoiceRequestError('')}>
          {invoiceRequestError}
        </Alert>
      )}

      {invoiceRequestSuccess && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setInvoiceRequestSuccess(false)}>
          Счет успешно создан!
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
              <TableCell sx={{ fontWeight: 600 }}>ID счета</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Дата выдачи</TableCell>
              <TableCell align="right" sx={{ fontWeight: 600 }}>Сумма</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {invoices.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} align="center" sx={{ py: 6 }}>
                  <Box sx={{ textAlign: 'center' }}>
                    <ReceiptIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2, opacity: 0.5 }} />
                    <Typography variant="body1" color="text.secondary" sx={{ fontWeight: 500 }}>
                      Нет счетов
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Запросите счет, нажав на кнопку выше
                    </Typography>
                  </Box>
                </TableCell>
              </TableRow>
            ) : (
              invoices.map((invoice) => (
                <TableRow
                  key={invoice.invoiceId}
                  sx={{
                    '&:hover': {
                      bgcolor: 'action.hover',
                    },
                    '&:last-child td': {
                      borderBottom: 0,
                    },
                  }}
                >
                  <TableCell sx={{ fontWeight: 500 }}>#{invoice.invoiceId}</TableCell>
                  <TableCell>{new Date(invoice.issueDate).toLocaleDateString('ru-RU', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600, fontSize: '1.1rem', color: 'success.main' }}>
                    {parseFloat(invoice.totalAmount).toLocaleString('ru-RU')} ₽
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог для получения информации об уборщике */}
      <Dialog open={openCleanerDialog} onClose={() => setOpenCleanerDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Узнать уборщика моего номера</DialogTitle>
        <DialogContent>
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

export default ClientPanel;

