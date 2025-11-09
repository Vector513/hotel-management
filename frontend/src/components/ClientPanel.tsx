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
    <Container maxWidth="md">
      <Box display="flex" justifyContent="space-between" alignItems="center" sx={{ mt: 3, mb: 2 }}>
        <Typography variant="h4" component="h1">
          Мои счета
        </Typography>
        <Box display="flex" gap={1}>
          <Button
            variant="contained"
            startIcon={<ReceiptIcon />}
            onClick={handleRequestInvoice}
            disabled={requestingInvoice}
          >
            {requestingInvoice ? 'Запрос...' : 'Запросить счет'}
          </Button>
          <Button
            variant="outlined"
            startIcon={<CleaningServicesIcon />}
            onClick={() => setOpenCleanerDialog(true)}
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

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID счета</TableCell>
              <TableCell>Дата выдачи</TableCell>
              <TableCell align="right">Сумма</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {invoices.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} align="center">
                  Нет счетов
                </TableCell>
              </TableRow>
            ) : (
              invoices.map((invoice) => (
                <TableRow key={invoice.invoiceId}>
                  <TableCell>{invoice.invoiceId}</TableCell>
                  <TableCell>{new Date(invoice.issueDate).toLocaleDateString('ru-RU')}</TableCell>
                  <TableCell align="right">{invoice.totalAmount} ₽</TableCell>
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
    </Container>
  );
};

export default ClientPanel;

