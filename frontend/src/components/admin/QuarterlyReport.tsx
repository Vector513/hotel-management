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
  Card,
  CardContent,
  Grid,
  TextField,
  Button,
  IconButton,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { reportsAPI } from '../../services/api';
import type { QuarterlyReport, RoomOccupancyInfo } from '../../types';
import { RoomType } from '../../types';

const roomTypeLabels: Record<RoomType, string> = {
  [RoomType.SINGLE]: 'Одноместный',
  [RoomType.DOUBLE]: 'Двухместный',
  [RoomType.TRIPLE]: 'Трехместный',
};

const QuarterlyReportComponent: React.FC = () => {
  const [report, setReport] = useState<QuarterlyReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [testDate, setTestDate] = useState<string>('');

  useEffect(() => {
    loadReport();
  }, []);

  const loadReport = async (testDateParam?: string) => {
    try {
      setLoading(true);
      setError('');
      const data = await reportsAPI.getQuarterlyReport(testDateParam);
      setReport(data);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки отчета');
    } finally {
      setLoading(false);
    }
  };

  const handleLoadWithTestDate = () => {
    if (testDate) {
      loadReport(testDate);
    } else {
      setError('Введите дату в формате YYYY-MM-DD');
    }
  };

  const handleReset = () => {
    setTestDate('');
    loadReport();
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ru-RU', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const calculateOccupancyRate = (occupied: number, total: number) => {
    if (total === 0) return 0;
    return ((occupied / total) * 100).toFixed(1);
  };

  if (loading) {
    return (
      <Container maxWidth="lg">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  if (error) {
    return (
      <Container maxWidth="lg">
        <Alert severity="error" sx={{ mt: 3 }}>
          {error}
        </Alert>
      </Container>
    );
  }

  if (!report) {
    return (
      <Container maxWidth="lg">
        <Alert severity="info" sx={{ mt: 3 }}>
          Нет данных для отчета
        </Alert>
      </Container>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
          <Box>
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
              Отчет за последний квартал
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Статистика работы отеля за отчетный период
            </Typography>
          </Box>
          <IconButton onClick={handleReset} title="Обновить отчет">
            <RefreshIcon />
          </IconButton>
        </Box>

        <Paper
          elevation={0}
          sx={{
            p: 2,
            mb: 3,
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: 2,
            bgcolor: 'rgba(245, 158, 11, 0.1)',
          }}
        >
          <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>
            🧪 Тестирование отчета
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Для проверки отчета за другой период введите тестовую дату (в формате YYYY-MM-DD).
            Отчет будет рассчитан для квартала, предшествующего указанной дате.
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
            <TextField
              label="Тестовая дата (YYYY-MM-DD)"
              value={testDate}
              onChange={(e) => setTestDate(e.target.value)}
              placeholder="2024-06-15"
              size="small"
              sx={{ minWidth: 200 }}
              helperText="Например: 2024-06-15 (для Q2 2024)"
            />
            <Button
              variant="outlined"
              onClick={handleLoadWithTestDate}
              disabled={!testDate}
              sx={{ minWidth: 150 }}
            >
              Загрузить с тестовой датой
            </Button>
            {testDate && (
              <Button
                variant="text"
                onClick={handleReset}
                size="small"
              >
                Сбросить
              </Button>
            )}
          </Box>
        </Paper>
      </Box>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={4}>
          <Card
            sx={{
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              color: 'white',
              height: '100%',
            }}
          >
            <CardContent>
              <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                Период отчета
              </Typography>
              <Typography variant="h6" sx={{ fontWeight: 600 }}>
                {formatDate(report.periodStart)}
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.9, mt: 0.5 }}>
                до {formatDate(report.periodEnd)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <Card
            sx={{
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
              color: 'white',
              height: '100%',
            }}
          >
            <CardContent>
              <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                Всего клиентов
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700 }}>
                {report.totalClients}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <Card
            sx={{
              background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
              color: 'white',
              height: '100%',
            }}
          >
            <CardContent>
              <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                Общий доход
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700 }}>
                {parseFloat(report.totalRevenue).toLocaleString('ru-RU')} ₽
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

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
              <TableCell sx={{ fontWeight: 600 }}>Номер</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Этаж</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Тип</TableCell>
              <TableCell align="right" sx={{ fontWeight: 600 }}>Занято дней</TableCell>
              <TableCell align="right" sx={{ fontWeight: 600 }}>Свободно дней</TableCell>
              <TableCell align="right" sx={{ fontWeight: 600 }}>Всего дней</TableCell>
              <TableCell align="right" sx={{ fontWeight: 600 }}>Процент занятости</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {report.roomOccupancy.map((room: RoomOccupancyInfo) => (
              <TableRow
                key={room.roomId}
                sx={{
                  '&:hover': {
                    bgcolor: 'action.hover',
                  },
                  '&:last-child td': {
                    borderBottom: 0,
                  },
                }}
              >
                <TableCell>{room.roomId}</TableCell>
                <TableCell sx={{ fontWeight: 500 }}>№{room.roomNumber}</TableCell>
                <TableCell>{room.floor}</TableCell>
                <TableCell>
                  <Chip
                    label={roomTypeLabels[room.type]}
                    size="small"
                    color="primary"
                    variant="outlined"
                    sx={{ fontWeight: 500 }}
                  />
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: 500, color: 'success.main' }}>
                  {room.occupiedDays}
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: 500, color: 'text.secondary' }}>
                  {room.freeDays}
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>
                  {room.totalDays}
                </TableCell>
                <TableCell align="right">
                  <Chip
                    label={`${calculateOccupancyRate(room.occupiedDays, room.totalDays)}%`}
                    size="small"
                    color={
                      parseFloat(calculateOccupancyRate(room.occupiedDays, room.totalDays)) > 70
                        ? 'success'
                        : parseFloat(calculateOccupancyRate(room.occupiedDays, room.totalDays)) > 40
                        ? 'warning'
                        : 'default'
                    }
                    sx={{ fontWeight: 600 }}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};

export default QuarterlyReportComponent;

