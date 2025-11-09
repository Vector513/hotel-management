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
import PeopleIcon from '@mui/icons-material/People';
import CleaningServicesIcon from '@mui/icons-material/CleaningServices';
import { roomsAPI, authAPI } from '../../services/api';
import type { Room, CreateRoomRequest, UpdateRoomRequest, Client } from '../../types';
import { RoomType, DayOfWeek } from '../../types';

const roomTypeLabels: Record<RoomType, string> = {
  [RoomType.SINGLE]: 'Одноместный',
  [RoomType.DOUBLE]: 'Двухместный',
  [RoomType.TRIPLE]: 'Трехместный',
};

const RoomsManagement: React.FC = () => {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [freeRooms, setFreeRooms] = useState<Room[]>([]);
  const [residents, setResidents] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [openResidentsDialog, setOpenResidentsDialog] = useState(false);
  const [openCleanerDialog, setOpenCleanerDialog] = useState(false);
  const [selectedRoomForCleaner, setSelectedRoomForCleaner] = useState<Room | null>(null);
  const [selectedDayOfWeek, setSelectedDayOfWeek] = useState<DayOfWeek>(DayOfWeek.MONDAY);
  const [cleanerName, setCleanerName] = useState<string>('');
  const [loadingCleaner, setLoadingCleaner] = useState(false);
  const [cleanerError, setCleanerError] = useState('');
  const [editingRoom, setEditingRoom] = useState<Room | null>(null);
  const [formData, setFormData] = useState<CreateRoomRequest>({
    roomNumber: 0,
    floor: 1,
    type: RoomType.SINGLE,
    pricePerDay: '0',
    phoneNumber: '',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [roomsData, freeRoomsData] = await Promise.all([
        roomsAPI.getAll(),
        roomsAPI.getFree(),
      ]);
      setRooms(roomsData);
      setFreeRooms(freeRoomsData.freeRooms);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки данных');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (room?: Room) => {
    if (room) {
      setEditingRoom(room);
      setFormData({
        roomNumber: room.roomNumber,
        floor: room.floor,
        type: room.type,
        pricePerDay: room.pricePerDay,
        phoneNumber: room.phoneNumber,
      });
    } else {
      setEditingRoom(null);
      setFormData({
        roomNumber: 0,
        floor: 1,
        type: RoomType.SINGLE,
        pricePerDay: '0',
        phoneNumber: '',
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingRoom(null);
  };

  const handleSubmit = async () => {
    try {
      if (editingRoom) {
        await roomsAPI.update(editingRoom.roomId, formData as UpdateRoomRequest);
      } else {
        await roomsAPI.create(formData);
      }
      await loadData();
      handleCloseDialog();
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка сохранения');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить этот номер?')) return;
    try {
      await roomsAPI.delete(id);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка удаления');
    }
  };

  const handleViewResidents = async (roomId: number) => {
    try {
      const data = await roomsAPI.getResidents(roomId);
      setResidents(data);
      setOpenResidentsDialog(true);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки жильцов');
    }
  };

  const handleOpenCleanerDialog = (room: Room) => {
    setSelectedRoomForCleaner(room);
    setSelectedDayOfWeek(DayOfWeek.MONDAY);
    setCleanerName('');
    setCleanerError('');
    setOpenCleanerDialog(true);
  };

  const handleCloseCleanerDialog = () => {
    setOpenCleanerDialog(false);
    setSelectedRoomForCleaner(null);
    setCleanerName('');
    setCleanerError('');
  };

  const handleGetCleaner = async () => {
    if (!selectedRoomForCleaner) return;

    try {
      setLoadingCleaner(true);
      setCleanerError('');
      const response = await authAPI.getRoomCleaner(
        selectedRoomForCleaner.roomId,
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

  if (loading && rooms.length === 0) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление номерами</Typography>
        <Box display="flex" gap={1}>
          <Chip
            label={`Свободно: ${freeRooms.length} из ${rooms.length}`}
            color="success"
            variant="outlined"
          />
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Добавить номер
          </Button>
        </Box>
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
              <TableCell>Номер</TableCell>
              <TableCell>Этаж</TableCell>
              <TableCell>Тип</TableCell>
              <TableCell>Цена за день</TableCell>
              <TableCell>Телефон</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rooms.map((room) => (
              <TableRow key={room.roomId}>
                <TableCell>{room.roomId}</TableCell>
                <TableCell>{room.roomNumber}</TableCell>
                <TableCell>{room.floor}</TableCell>
                <TableCell>{roomTypeLabels[room.type]}</TableCell>
                <TableCell>{room.pricePerDay} ₽</TableCell>
                <TableCell>{room.phoneNumber}</TableCell>
                <TableCell>
                  <IconButton
                    size="small"
                    onClick={() => handleOpenCleanerDialog(room)}
                    title="Узнать уборщика"
                  >
                    <CleaningServicesIcon />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => handleViewResidents(room.roomId)}
                    title="Жильцы"
                  >
                    <PeopleIcon />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleOpenDialog(room)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDelete(room.roomId)}>
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingRoom ? 'Редактировать номер' : 'Добавить номер'}</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Номер комнаты"
            type="number"
            value={formData.roomNumber}
            onChange={(e) => setFormData({ ...formData, roomNumber: parseInt(e.target.value) || 0 })}
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
          <TextField
            fullWidth
            select
            label="Тип"
            value={formData.type}
            onChange={(e) => setFormData({ ...formData, type: e.target.value as RoomType })}
            margin="normal"
            required
          >
            {Object.values(RoomType).map((type) => (
              <MenuItem key={type} value={type}>
                {roomTypeLabels[type]}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            fullWidth
            label="Цена за день"
            type="number"
            value={formData.pricePerDay}
            onChange={(e) => setFormData({ ...formData, pricePerDay: e.target.value })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Телефон"
            value={formData.phoneNumber}
            onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
            margin="normal"
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Отмена</Button>
          <Button onClick={handleSubmit} variant="contained">
            {editingRoom ? 'Сохранить' : 'Создать'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={openResidentsDialog}
        onClose={() => setOpenResidentsDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Жильцы номера</DialogTitle>
        <DialogContent>
          {residents.length === 0 ? (
            <Typography>Нет жильцов</Typography>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>ФИО</TableCell>
                  <TableCell>Паспорт</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {residents.map((client) => (
                  <TableRow key={client.clientId}>
                    <TableCell>{client.clientId}</TableCell>
                    <TableCell>{client.fullName}</TableCell>
                    <TableCell>{client.passportNumber}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenResidentsDialog(false)}>Закрыть</Button>
        </DialogActions>
      </Dialog>

      {/* Диалог для получения информации об уборщике */}
      <Dialog open={openCleanerDialog} onClose={handleCloseCleanerDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Узнать уборщика номера</DialogTitle>
        <DialogContent>
          {selectedRoomForCleaner && (
            <>
              <Typography variant="body2" sx={{ mb: 2 }}>
                Номер: <strong>№{selectedRoomForCleaner.roomNumber}</strong> (Этаж {selectedRoomForCleaner.floor})
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

export default RoomsManagement;

