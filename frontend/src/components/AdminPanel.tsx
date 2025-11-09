import React, { useState } from 'react';
import {
  Container,
  Tabs,
  Tab,
  Box,
  Typography,
} from '@mui/material';
import ClientsManagement from './admin/ClientsManagement';
import RoomsManagement from './admin/RoomsManagement';
import InvoicesManagement from './admin/InvoicesManagement';
import EmployeesManagement from './admin/EmployeesManagement';
import SchedulesManagement from './admin/SchedulesManagement';
import UsersManagement from './admin/UsersManagement';

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
    <Container maxWidth="xl">
      <Typography variant="h4" component="h1" gutterBottom sx={{ mt: 3, mb: 2 }}>
        Панель администратора
      </Typography>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={value} onChange={handleChange} aria-label="admin tabs">
          <Tab label="Клиенты" />
          <Tab label="Номера" />
          <Tab label="Счета" />
          <Tab label="Сотрудники" />
          <Tab label="Расписание уборки" />
          <Tab label="Пользователи" />
        </Tabs>
      </Box>

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
    </Container>
  );
};

export default AdminPanel;

