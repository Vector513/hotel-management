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
import SearchIcon from '@mui/icons-material/Search';
import ReceiptIcon from '@mui/icons-material/Receipt';
import { clientsAPI, roomsAPI, invoicesAPI } from '../../services/api';
import type { Client, CreateClientRequest, UpdateClientRequest, Room } from '../../types';

const ClientsManagement: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [editingClient, setEditingClient] = useState<Client | null>(null);
  const [formData, setFormData] = useState<CreateClientRequest>({
    passportNumber: '',
    fullName: '',
    city: '',
    checkInDate: new Date().toISOString().split('T')[0],
    daysReserved: 1,
    roomId: 0,
  });
  const [searchCity, setSearchCity] = useState('');
  const [showCredentials, setShowCredentials] = useState(false);
  const [credentials, setCredentials] = useState<{ login: string; password: string } | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [clientsData, roomsData] = await Promise.all([
        clientsAPI.getAll(),
        roomsAPI.getAll(),
      ]);
      setClients(clientsData);
      setRooms(roomsData);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки данных');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (client?: Client) => {
    if (client) {
      setEditingClient(client);
      setFormData({
        passportNumber: client.passportNumber,
        fullName: client.fullName,
        city: client.city,
        checkInDate: client.checkInDate,
        daysReserved: client.daysReserved,
        roomId: client.roomId,
      });
    } else {
      setEditingClient(null);
      setFormData({
        passportNumber: '',
        fullName: '',
        city: '',
        checkInDate: new Date().toISOString().split('T')[0],
        daysReserved: 1,
        roomId: rooms[0]?.roomId || 0,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingClient(null);
    setCredentials(null);
    setShowCredentials(false);
  };

  const handleSubmit = async () => {
    try {
      if (editingClient) {
        await clientsAPI.update(editingClient.clientId, formData as UpdateClientRequest);
      } else {
        const response = await clientsAPI.create(formData);
        setCredentials({ login: response.login, password: response.password });
        setShowCredentials(true);
      }
      await loadData();
      if (!showCredentials) {
        handleCloseDialog();
      }
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка сохранения');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить этого клиента?')) return;
    try {
      await clientsAPI.delete(id);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка удаления');
    }
  };

  const handleCreateInvoice = async (clientId: number) => {
    try {
      const response = await invoicesAPI.createForClient(clientId);
      alert(`Счет создан! ID: ${response.invoiceId}, Сумма: ${response.amount} ₽`);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка создания счета');
    }
  };

  const handleSearchByCity = async () => {
    if (!searchCity.trim()) {
      loadData();
      return;
    }
    try {
      setLoading(true);
      const data = await clientsAPI.getByCity(searchCity);
      setClients(data);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка поиска');
    } finally {
      setLoading(false);
    }
  };

  if (loading && clients.length === 0) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление клиентами</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Добавить клиента
        </Button>
      </Box>

      <Box display="flex" gap={2} mb={2}>
        <TextField
          label="Поиск по городу"
          value={searchCity}
          onChange={(e) => setSearchCity(e.target.value)}
          size="small"
        />
        <Button
          variant="outlined"
          startIcon={<SearchIcon />}
          onClick={handleSearchByCity}
        >
          Найти
        </Button>
        <Button variant="outlined" onClick={loadData}>
          Показать всех
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
              <TableCell>Паспорт</TableCell>
              <TableCell>Город</TableCell>
              <TableCell>Дата заезда</TableCell>
              <TableCell>Дней</TableCell>
              <TableCell>Номер</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {clients.map((client) => (
              <TableRow key={client.clientId}>
                <TableCell>{client.clientId}</TableCell>
                <TableCell>{client.fullName}</TableCell>
                <TableCell>{client.passportNumber}</TableCell>
                <TableCell>{client.city}</TableCell>
                <TableCell>{new Date(client.checkInDate).toLocaleDateString('ru-RU')}</TableCell>
                <TableCell>{client.daysReserved}</TableCell>
                <TableCell>{client.roomId}</TableCell>
                <TableCell>
                  <IconButton
                    size="small"
                    onClick={() => handleCreateInvoice(client.clientId)}
                    title="Создать счет"
                  >
                    <ReceiptIcon />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleOpenDialog(client)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDelete(client.clientId)}>
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingClient ? 'Редактировать клиента' : 'Добавить клиента'}</DialogTitle>
        <DialogContent>
          {showCredentials && credentials && (
            <Alert severity="success" sx={{ mb: 2 }}>
              Клиент создан! Логин: {credentials.login}, Пароль: {credentials.password}
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
            label="Номер паспорта"
            value={formData.passportNumber}
            onChange={(e) => setFormData({ ...formData, passportNumber: e.target.value })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Город"
            value={formData.city}
            onChange={(e) => setFormData({ ...formData, city: e.target.value })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Дата заезда"
            type="date"
            value={formData.checkInDate}
            onChange={(e) => setFormData({ ...formData, checkInDate: e.target.value })}
            margin="normal"
            InputLabelProps={{ shrink: true }}
            required
          />
          <TextField
            fullWidth
            label="Количество дней"
            type="number"
            value={formData.daysReserved}
            onChange={(e) => setFormData({ ...formData, daysReserved: parseInt(e.target.value) || 1 })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            select
            label="Номер"
            value={formData.roomId}
            onChange={(e) => setFormData({ ...formData, roomId: parseInt(e.target.value) })}
            margin="normal"
            required
          >
            {rooms.map((room) => (
              <MenuItem key={room.roomId} value={room.roomId}>
                №{room.roomNumber} (Этаж {room.floor}, {room.type})
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Отмена</Button>
          <Button onClick={handleSubmit} variant="contained">
            {editingClient ? 'Сохранить' : 'Создать'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ClientsManagement;

