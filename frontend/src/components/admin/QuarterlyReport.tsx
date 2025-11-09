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
} from '@mui/material';
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

  useEffect(() => {
    loadReport();
  }, []);

  const loadReport = async () => {
    try {
      setLoading(true);
      const data = await reportsAPI.getQuarterlyReport();
      setReport(data);
    } catch (err: any) {
      setError(err.response?.data || 'Ошибка загрузки отчета');
    } finally {
      setLoading(false);
    }
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
    <Container maxWidth="lg">
      <Typography variant="h4" component="h1" gutterBottom sx={{ mt: 3, mb: 3 }}>
        Отчет за последний квартал
      </Typography>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Период отчета
              </Typography>
              <Typography variant="h6">
                {formatDate(report.periodStart)} - {formatDate(report.periodEnd)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Всего клиентов
              </Typography>
              <Typography variant="h6">{report.totalClients}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Общий доход
              </Typography>
              <Typography variant="h6">{parseFloat(report.totalRevenue).toLocaleString('ru-RU')} ₽</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Номер</TableCell>
              <TableCell>Этаж</TableCell>
              <TableCell>Тип</TableCell>
              <TableCell align="right">Занято дней</TableCell>
              <TableCell align="right">Свободно дней</TableCell>
              <TableCell align="right">Всего дней</TableCell>
              <TableCell align="right">Процент занятости</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {report.roomOccupancy.map((room: RoomOccupancyInfo) => (
              <TableRow key={room.roomId}>
                <TableCell>{room.roomId}</TableCell>
                <TableCell>{room.roomNumber}</TableCell>
                <TableCell>{room.floor}</TableCell>
                <TableCell>
                  <Chip
                    label={roomTypeLabels[room.type]}
                    size="small"
                    color="primary"
                    variant="outlined"
                  />
                </TableCell>
                <TableCell align="right">{room.occupiedDays}</TableCell>
                <TableCell align="right">{room.freeDays}</TableCell>
                <TableCell align="right">{room.totalDays}</TableCell>
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
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
};

export default QuarterlyReportComponent;

