import React, { useState } from 'react';
import {
  Tabs,
  Tab,
  Box,
  Typography,
  Paper,
} from '@mui/material';
import ClientsManagement from './admin/ClientsManagement';
import RoomsManagement from './admin/RoomsManagement';
import InvoicesManagement from './admin/InvoicesManagement';
import EmployeesManagement from './admin/EmployeesManagement';
import SchedulesManagement from './admin/SchedulesManagement';
import UsersManagement from './admin/UsersManagement';
import QuarterlyReport from './admin/QuarterlyReport';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`admin-tabpanel-${index}`}
      aria-labelledby={`admin-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const AdminPanel: React.FC = () => {
  const [value, setValue] = useState(0);

  const handleChange = (_event: React.SyntheticEvent, newValue: number) => {
    setValue(newValue);
  };

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
          Панель администратора
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Управление отелем и всеми процессами
        </Typography>
      </Box>

      <Paper
        elevation={0}
        sx={{
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 3,
          overflow: 'hidden',
        }}
      >
        <Tabs
          value={value}
          onChange={handleChange}
          aria-label="admin tabs"
          variant="scrollable"
          scrollButtons="auto"
          sx={{
            borderBottom: 1,
            borderColor: 'divider',
            '& .MuiTab-root': {
              minHeight: 64,
              fontWeight: 500,
              textTransform: 'none',
              fontSize: '0.9375rem',
              '&.Mui-selected': {
                color: 'primary.main',
                fontWeight: 600,
              },
            },
            '& .MuiTabs-indicator': {
              height: 3,
              borderRadius: '3px 3px 0 0',
            },
          }}
        >
          <Tab label="Клиенты" />
          <Tab label="Номера" />
          <Tab label="Счета" />
          <Tab label="Сотрудники" />
          <Tab label="Расписание уборки" />
          <Tab label="Пользователи" />
          <Tab label="Отчет за квартал" />
        </Tabs>

        <TabPanel value={value} index={0}>
          <ClientsManagement />
        </TabPanel>
        <TabPanel value={value} index={1}>
          <RoomsManagement />
        </TabPanel>
        <TabPanel value={value} index={2}>
          <InvoicesManagement />
        </TabPanel>
        <TabPanel value={value} index={3}>
          <EmployeesManagement />
        </TabPanel>
        <TabPanel value={value} index={4}>
          <SchedulesManagement />
        </TabPanel>
        <TabPanel value={value} index={5}>
          <UsersManagement />
        </TabPanel>
        <TabPanel value={value} index={6}>
          <QuarterlyReport />
        </TabPanel>
      </Paper>
    </Box>
  );
};

export default AdminPanel;

