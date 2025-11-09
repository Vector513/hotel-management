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
import CleaningServicesIcon from '@mui/icons-material/CleaningServices';
import { clientsAPI, roomsAPI, invoicesAPI, authAPI } from '../../services/api';
import type { Client, CreateClientRequest, UpdateClientRequest, Room } from '../../types';
import { DayOfWeek } from '../../types';

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
  const [openCleanerDialog, setOpenCleanerDialog] = useState(false);
  const [selectedClientForCleaner, setSelectedClientForCleaner] = useState<Client | null>(null);
  const [selectedDayOfWeek, setSelectedDayOfWeek] = useState<DayOfWeek>(DayOfWeek.MONDAY);
  const [cleanerName, setCleanerName] = useState<string>('');
  const [loadingCleaner, setLoadingCleaner] = useState(false);
  const [cleanerError, setCleanerError] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  // Функция для получения максимальной вместимости номера
  const getMaxCapacity = (roomType: string): number => {
    switch (roomType) {
      case 'SINGLE': return 1;
      case 'DOUBLE': return 2;
      case 'TRIPLE': return 3;
      default: return 0;
    }
  };

  // Функция для получения количества жильцов в номере
  const getResidentsCount = (roomId: number): number => {
    return clients.filter(c => c.roomId === roomId && c.isResident).length;
  };

  // Функция для проверки, доступен ли номер
  const isRoomAvailable = (room: Room, editingClientId?: number): boolean => {
    const residentsCount = clients.filter(
      c => c.roomId === room.roomId && c.isResident && c.clientId !== editingClientId
    ).length;
    const maxCapacity = getMaxCapacity(room.type);
    return residentsCount < maxCapacity;
  };

  // Получаем доступные номера для выбора
  const getAvailableRooms = (editingClientId?: number): Room[] => {
    return rooms.filter(room => isRoomAvailable(room, editingClientId));
  };

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
      const availableRooms = getAvailableRooms();
      setFormData({
        passportNumber: '',
        fullName: '',
        city: '',
        checkInDate: new Date().toISOString().split('T')[0],
        daysReserved: 1,
        roomId: availableRooms[0]?.roomId || 0,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingClient(null);
    setCredentials(null);
    setShowCredentials(false);
    // Сбрасываем форму
    const availableRooms = getAvailableRooms();
    setFormData({
      passportNumber: '',
      fullName: '',
      city: '',
      checkInDate: new Date().toISOString().split('T')[0],
      daysReserved: 1,
      roomId: availableRooms[0]?.roomId || 0,
    });
  };

  const handleSubmit = async () => {
    try {
      if (editingClient) {
        await clientsAPI.update(editingClient.clientId, formData as UpdateClientRequest);
        await loadData();
        handleCloseDialog();
      } else {
        const response = await clientsAPI.create(formData);
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

  const handleOpenCleanerDialog = (client: Client) => {
    setSelectedClientForCleaner(client);
    setSelectedDayOfWeek(DayOfWeek.MONDAY);
    setCleanerName('');
    setCleanerError('');
    setOpenCleanerDialog(true);
  };

  const handleCloseCleanerDialog = () => {
    setOpenCleanerDialog(false);
    setSelectedClientForCleaner(null);
    setCleanerName('');
    setCleanerError('');
  };

  const handleGetCleaner = async () => {
    if (!selectedClientForCleaner) return;

    try {
      setLoadingCleaner(true);
      setCleanerError('');
      const response = await authAPI.getClientCleaner(
        selectedClientForCleaner.clientId,
        selectedDayOfWeek
      );
      setCleanerName(response.employeeName);
    } catch (err: any) {
      setCleanerError(err.response?.data || 'Ошибка получения информации об уборщике');
      setCleanerName('');
    } finally {
      setLoadingCleaner(false);
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
                    onClick={() => handleOpenCleanerDialog(client)}
                    title="Узнать уборщика"
                  >
                    <CleaningServicesIcon />
                  </IconButton>
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
              <Typography variant="subtitle2" gutterBottom>
                Клиент создан!
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
            helperText={
              formData.roomId > 0
                ? (() => {
                    const room = rooms.find(r => r.roomId === formData.roomId);
                    if (!room) return '';
                    const residents = getResidentsCount(room.roomId);
                    const maxCapacity = getMaxCapacity(room.type);
                    const available = maxCapacity - residents;
                    return `Занято: ${residents}/${maxCapacity}. ${available > 0 ? `Свободно: ${available}` : 'Номер заполнен'}`;
                  })()
                : ''
            }
          >
            {getAvailableRooms(editingClient?.clientId).map((room) => {
              const residents = getResidentsCount(room.roomId);
              const maxCapacity = getMaxCapacity(room.type);
              const available = maxCapacity - residents;
              return (
                <MenuItem key={room.roomId} value={room.roomId}>
                  №{room.roomNumber} (Этаж {room.floor}, {room.type}) - {available > 0 ? `Свободно: ${available}/${maxCapacity}` : 'Заполнен'}
                </MenuItem>
              );
            })}
            {/* Показываем текущий номер клиента, даже если он заполнен (при редактировании) */}
            {editingClient && (() => {
              const currentRoom = rooms.find(r => r.roomId === editingClient.roomId);
              if (currentRoom && !isRoomAvailable(currentRoom, editingClient.clientId)) {
                return (
                  <MenuItem key={`current-${editingClient.roomId}`} value={editingClient.roomId}>
                    №{currentRoom.roomNumber} (текущий номер)
                  </MenuItem>
                );
              }
              return null;
            })()}
            {getAvailableRooms(editingClient?.clientId).length === 0 && !editingClient && (
              <MenuItem disabled>
                Нет доступных номеров
              </MenuItem>
            )}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>{showCredentials ? 'Закрыть' : 'Отмена'}</Button>
          {!showCredentials && (
            <Button onClick={handleSubmit} variant="contained">
              {editingClient ? 'Сохранить' : 'Создать'}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Диалог для получения информации об уборщике */}
      <Dialog open={openCleanerDialog} onClose={handleCloseCleanerDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Узнать уборщика номера</DialogTitle>
        <DialogContent>
          {selectedClientForCleaner && (
            <>
              <Typography variant="body2" sx={{ mb: 2 }}>
                Клиент: <strong>{selectedClientForCleaner.fullName}</strong>
              </Typography>
              <TextField
                fullWidth
                select
                label="День недели"
                value={selectedDayOfWeek}
                onChange={(e) => setSelectedDayOfWeek(e.target.value as DayOfWeek)}
                margin="normal"
                required
              >
                {Object.values(DayOfWeek).map((day) => {
                  const dayLabels: Record<DayOfWeek, string> = {
                    [DayOfWeek.MONDAY]: 'Понедельник',
                    [DayOfWeek.TUESDAY]: 'Вторник',
                    [DayOfWeek.WEDNESDAY]: 'Среда',
                    [DayOfWeek.THURSDAY]: 'Четверг',
                    [DayOfWeek.FRIDAY]: 'Пятница',
                    [DayOfWeek.SATURDAY]: 'Суббота',
                    [DayOfWeek.SUNDAY]: 'Воскресенье',
                  };
                  return (
                    <MenuItem key={day} value={day}>
                      {dayLabels[day]}
                    </MenuItem>
                  );
                })}
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
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseCleanerDialog}>Закрыть</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ClientsManagement;

