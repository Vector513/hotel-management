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
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import ReceiptIcon from '@mui/icons-material/Receipt';
import { invoicesAPI, clientsAPI } from '../../services/api';
import type { Invoice, CreateInvoiceRequest, UpdateInvoiceRequest, Client } from '../../types';

const InvoicesManagement: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [editingInvoice, setEditingInvoice] = useState<Invoice | null>(null);
  const [formData, setFormData] = useState<CreateInvoiceRequest>({
    clientId: 0,
    totalAmount: '0',
    issueDate: new Date().toISOString().split('T')[0],
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [invoicesData, clientsData] = await Promise.all([
        invoicesAPI.getAll(),
        clientsAPI.getAll(),
      ]);
      setInvoices(invoicesData);
      setClients(clientsData);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки данных');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (invoice?: Invoice) => {
    if (invoice) {
      setEditingInvoice(invoice);
      setFormData({
        clientId: invoice.clientId,
        totalAmount: invoice.totalAmount,
        issueDate: invoice.issueDate,
      });
    } else {
      setEditingInvoice(null);
      setFormData({
        clientId: clients[0]?.clientId || 0,
        totalAmount: '0',
        issueDate: new Date().toISOString().split('T')[0],
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingInvoice(null);
  };

  const handleSubmit = async () => {
    try {
      if (editingInvoice) {
        await invoicesAPI.update(editingInvoice.invoiceId, formData as UpdateInvoiceRequest);
      } else {
        await invoicesAPI.create(formData);
      }
      await loadData();
      handleCloseDialog();
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка сохранения');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить этот счет?')) return;
    try {
      await invoicesAPI.delete(id);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка удаления');
    }
  };

  if (loading && invoices.length === 0) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление счетами</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Добавить счет
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
              <TableCell>ID клиента</TableCell>
              <TableCell>Сумма</TableCell>
              <TableCell>Дата выдачи</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {invoices.map((invoice) => (
              <TableRow key={invoice.invoiceId}>
                <TableCell>{invoice.invoiceId}</TableCell>
                <TableCell>{invoice.clientId}</TableCell>
                <TableCell>{invoice.totalAmount} ₽</TableCell>
                <TableCell>{new Date(invoice.issueDate).toLocaleDateString('ru-RU')}</TableCell>
                <TableCell>
                  <IconButton size="small" onClick={() => handleOpenDialog(invoice)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDelete(invoice.invoiceId)}>
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingInvoice ? 'Редактировать счет' : 'Добавить счет'}</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            select
            label="Клиент"
            value={formData.clientId}
            onChange={(e) => setFormData({ ...formData, clientId: parseInt(e.target.value) })}
            margin="normal"
            required
          >
            {clients.map((client) => (
              <MenuItem key={client.clientId} value={client.clientId}>
                {client.fullName} (ID: {client.clientId})
              </MenuItem>
            ))}
          </TextField>
          <TextField
            fullWidth
            label="Сумма"
            type="number"
            value={formData.totalAmount}
            onChange={(e) => setFormData({ ...formData, totalAmount: e.target.value })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Дата выдачи"
            type="date"
            value={formData.issueDate}
            onChange={(e) => setFormData({ ...formData, issueDate: e.target.value })}
            margin="normal"
            InputLabelProps={{ shrink: true }}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Отмена</Button>
          <Button onClick={handleSubmit} variant="contained">
            {editingInvoice ? 'Сохранить' : 'Создать'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default InvoicesManagement;

