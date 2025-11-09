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
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mb={3}
        flexWrap="wrap"
        gap={2}
      >
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 600, mb: 0.5 }}>
            Управление клиентами
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Всего клиентов: {clients.length}
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
          sx={{
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            '&:hover': {
              background: 'linear-gradient(135deg, #5568d3 0%, #6a3d8f 100%)',
              boxShadow: '0 10px 25px rgba(102, 126, 234, 0.4)',
            },
          }}
        >
          Добавить клиента
        </Button>
      </Box>

      <Paper
        elevation={0}
        sx={{
          p: 2,
          mb: 3,
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 2,
        }}
      >
        <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
          <TextField
            label="Поиск по городу"
            value={searchCity}
            onChange={(e) => setSearchCity(e.target.value)}
            size="small"
            sx={{ flexGrow: 1, minWidth: 200 }}
            InputProps={{
              startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />,
            }}
          />
          <Button
            variant="outlined"
            onClick={handleSearchByCity}
            sx={{ minWidth: 120 }}
          >
            Найти
          </Button>
          <Button
            variant="outlined"
            onClick={loadData}
            sx={{ minWidth: 140 }}
          >
            Показать всех
          </Button>
        </Box>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
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
              <TableCell sx={{ fontWeight: 600 }}>ID</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>ФИО</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Паспорт</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Город</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Дата заезда</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Дней</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Номер</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {clients.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                  <Typography variant="body2" color="text.secondary">
                    Нет клиентов
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              clients.map((client) => (
                <TableRow
                  key={client.clientId}
                  sx={{
                    '&:hover': {
                      bgcolor: 'action.hover',
                    },
                    '&:last-child td': {
                      borderBottom: 0,
                    },
                  }}
                >
                  <TableCell>{client.clientId}</TableCell>
                  <TableCell sx={{ fontWeight: 500 }}>{client.fullName}</TableCell>
                  <TableCell>{client.passportNumber}</TableCell>
                  <TableCell>{client.city}</TableCell>
                  <TableCell>{new Date(client.checkInDate).toLocaleDateString('ru-RU')}</TableCell>
                  <TableCell>{client.daysReserved}</TableCell>
                  <TableCell>{client.roomId}</TableCell>
                  <TableCell>
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                      <IconButton
                        size="small"
                        onClick={() => handleOpenCleanerDialog(client)}
                        title="Узнать уборщика"
                        sx={{
                          color: 'info.main',
                          '&:hover': { bgcolor: 'info.light', color: 'white' },
                        }}
                      >
                        <CleaningServicesIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        onClick={() => handleCreateInvoice(client.clientId)}
                        title="Создать счет"
                        sx={{
                          color: 'success.main',
                          '&:hover': { bgcolor: 'success.light', color: 'white' },
                        }}
                      >
                        <ReceiptIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        onClick={() => handleOpenDialog(client)}
                        sx={{
                          color: 'primary.main',
                          '&:hover': { bgcolor: 'primary.light', color: 'white' },
                        }}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        onClick={() => handleDelete(client.clientId)}
                        sx={{
                          color: 'error.main',
                          '&:hover': { bgcolor: 'error.light', color: 'white' },
                        }}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
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

