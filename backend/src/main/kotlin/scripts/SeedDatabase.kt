package com.example.scripts

import com.example.database.dao.*
import com.example.models.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

object SeedDatabase {
    
    private val russianFirstNames = listOf(
        "Иван", "Александр", "Сергей", "Дмитрий", "Андрей", "Алексей", "Максим", "Владимир",
        "Евгений", "Николай", "Михаил", "Павел", "Роман", "Олег", "Артем", "Илья",
        "Анна", "Мария", "Елена", "Ольга", "Татьяна", "Наталья", "Ирина", "Светлана",
        "Екатерина", "Юлия", "Анастасия", "Дарья", "Виктория", "Полина", "София", "Александра"
    )
    
    private val russianLastNames = listOf(
        "Иванов", "Петров", "Смирнов", "Кузнецов", "Попов", "Соколов", "Лебедев", "Козлов",
        "Новиков", "Морозов", "Петров", "Волков", "Соловьев", "Васильев", "Зайцев", "Павлов",
        "Семенов", "Голубев", "Виноградов", "Богданов", "Воробьев", "Федоров", "Михайлов",
        "Белов", "Тарасов", "Беляев", "Комаров", "Орлов", "Киселев", "Макаров", "Андреев"
    )
    
    private val russianMiddleNames = listOf(
        "Иванович", "Александрович", "Сергеевич", "Дмитриевич", "Андреевич", "Алексеевич",
        "Максимович", "Владимирович", "Евгеньевич", "Николаевич", "Михайлович", "Павлович",
        "Ивановна", "Александровна", "Сергеевна", "Дмитриевна", "Андреевна", "Алексеевна",
        "Максимовна", "Владимировна", "Евгеньевна", "Николаевна", "Михайловна", "Павловна"
    )
    
    private val cities = listOf(
        "Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань", "Нижний Новгород",
        "Челябинск", "Самара", "Омск", "Ростов-на-Дону", "Уфа", "Красноярск", "Воронеж",
        "Пермь", "Волгоград", "Краснодар", "Саратов", "Тюмень", "Тольятти", "Ижевск"
    )
    
    fun seed() {
        println("Начинаем заполнение базы данных...")
        
        // Создаем админа
        val adminUser = createAdmin()
        println("✓ Создан администратор: ${adminUser.username} / admin123")
        
        // Создаем номера
        val rooms = createRooms()
        println("✓ Создано ${rooms.size} номеров")
        
        // Создаем работников
        val employees = createEmployees()
        println("✓ Создано ${employees.size} работников")
        
        // Создаем расписание уборки
        createCleaningSchedules(employees)
        println("✓ Создано расписание уборки")
        
        // Создаем клиентов
        val clients = createClients(rooms)
        println("✓ Создано ${clients.size} клиентов")
        
        // Создаем счета для некоторых клиентов
        createInvoices(clients)
        println("✓ Создано несколько счетов")
        
        println("\n✅ База данных успешно заполнена!")
        println("\nДанные для входа:")
        println("Администратор: admin / admin123")
        println("Клиенты: client1, client2, ... / password123")
        println("Работники: worker1, worker2, ... / password123")
    }
    
    private fun createAdmin(): User {
        val userDao = UserDao()
        val existingAdmin = userDao.findByUsername("admin")
        if (existingAdmin != null) {
            return existingAdmin
        }
        
        return userDao.create(
            username = "admin",
            password = "admin123",
            role = UserRole.ADMIN
        ) ?: throw RuntimeException("Не удалось создать администратора")
    }
    
    private fun createRooms(): List<Room> {
        val roomDao = RoomDao()
        val rooms = mutableListOf<Room>()
        
        // Создаем номера на разных этажах (1-5 этажи)
        var roomNumber = 101
        for (floor in 1..5) {
            // На каждом этаже по 6 номеров
            for (i in 1..6) {
                val type = when (i % 3) {
                    0 -> RoomType.SINGLE
                    1 -> RoomType.DOUBLE
                    else -> RoomType.TRIPLE
                }
                
                val pricePerDay = when (type) {
                    RoomType.SINGLE -> BigDecimal("2500.00")
                    RoomType.DOUBLE -> BigDecimal("3500.00")
                    RoomType.TRIPLE -> BigDecimal("4500.00")
                }
                
                val phoneNumber = "+7 (812) ${String.format("%03d", roomNumber)}-${String.format("%02d", floor)}-${String.format("%02d", i)}"
                
                val room = Room(
                    roomId = 0,
                    roomNumber = roomNumber,
                    floor = floor,
                    type = type,
                    pricePerDay = pricePerDay,
                    phoneNumber = phoneNumber
                )
                
                val roomId = roomDao.add(room)
                rooms.add(room.copy(roomId = roomId))
                roomNumber++
            }
        }
        
        return rooms
    }
    
    private fun createEmployees(): List<Employee> {
        val employeeDao = EmployeeDao()
        val userDao = UserDao()
        val employees = mutableListOf<Employee>()
        
        // Создаем по 2 работника на каждый этаж (1-5 этажи)
        for (floor in 1..5) {
            for (i in 1..2) {
                val firstName = russianFirstNames.random()
                val lastName = russianLastNames.random()
                val fullName = "$lastName $firstName"
                
                val employee = Employee(
                    employeeId = 0,
                    fullName = fullName,
                    floor = floor
                )
                
                val employeeId = employeeDao.add(employee)
                val createdEmployee = employee.copy(employeeId = employeeId)
                employees.add(createdEmployee)
                
                // Создаем пользователя для работника
                val username = "worker${employeeId}"
                userDao.createForEmployee(employeeId, username, "password123")
            }
        }
        
        return employees
    }
    
    private fun createCleaningSchedules(employees: List<Employee>) {
        val scheduleDao = CleaningScheduleDao()
        val daysOfWeek = DayOfWeek.values()
        
        // Распределяем работников по дням недели для каждого этажа
        val employeesByFloor = employees.groupBy { it.floor }
        
        employeesByFloor.forEach { (floor, floorEmployees) ->
            daysOfWeek.forEachIndexed { dayIndex, day ->
                // Каждый работник убирает свой этаж в определенные дни
                val employeeIndex = dayIndex % floorEmployees.size
                val employee = floorEmployees[employeeIndex]
                
                val schedule = CleaningSchedule(
                    scheduleId = 0,
                    employeeId = employee.employeeId,
                    floor = floor,
                    dayOfWeek = day
                )
                
                scheduleDao.add(schedule)
            }
        }
    }
    
    private fun createClients(rooms: List<Room>): List<Client> {
        val clientDao = ClientDao()
        val userDao = UserDao()
        val clients = mutableListOf<Client>()
        val random = Random()
        
        // Создаем 50 клиентов
        for (i in 1..50) {
            val firstName = russianFirstNames.random()
            val lastName = russianLastNames.random()
            val middleName = russianMiddleNames.random()
            val fullName = "$lastName $firstName $middleName"
            
            val passportNumber = "${random.nextInt(1000, 9999)} ${random.nextInt(100000, 999999)}"
            val city = cities.random()
            
            // Случайная дата заезда в последние 30 дней или будущие 30 дней
            val daysOffset = random.nextInt(-30, 30)
            val checkInDate = LocalDate.now().plusDays(daysOffset.toLong())
            val daysReserved = random.nextInt(1, 14) // От 1 до 14 дней
            
            // Выбираем случайный номер, учитывая тип
            val availableRooms = rooms.filter { room ->
                val currentResidents = clients.count { 
                    it.roomId == room.roomId && it.isResident 
                }
                currentResidents < room.type.getMaxCapacity()
            }
            
            if (availableRooms.isEmpty()) {
                // Если все номера заняты, выбираем любой
                val room = rooms.random()
                val client = Client(
                    clientId = 0,
                    passportNumber = passportNumber,
                    fullName = fullName,
                    city = city,
                    checkInDate = checkInDate,
                    daysReserved = daysReserved,
                    roomId = room.roomId,
                    isResident = checkInDate <= LocalDate.now() && 
                                 checkInDate.plusDays(daysReserved.toLong()) >= LocalDate.now()
                )
                
                val clientId = clientDao.add(client)
                val createdClient = client.copy(clientId = clientId)
                clients.add(createdClient)
                
                // Создаем пользователя для клиента
                val username = "client$clientId"
                userDao.createForClient(clientId, username, "password123")
            } else {
                val room = availableRooms.random()
                val client = Client(
                    clientId = 0,
                    passportNumber = passportNumber,
                    fullName = fullName,
                    city = city,
                    checkInDate = checkInDate,
                    daysReserved = daysReserved,
                    roomId = room.roomId,
                    isResident = checkInDate <= LocalDate.now() && 
                                 checkInDate.plusDays(daysReserved.toLong()) >= LocalDate.now()
                )
                
                val clientId = clientDao.add(client)
                val createdClient = client.copy(clientId = clientId)
                clients.add(createdClient)
                
                // Создаем пользователя для клиента
                val username = "client$clientId"
                userDao.createForClient(clientId, username, "password123")
            }
        }
        
        return clients
    }
    
    private fun createInvoices(clients: List<Client>) {
        val invoiceDao = InvoiceDao()
        val roomDao = RoomDao()
        val random = Random()
        
        // Создаем счета для 30% клиентов
        val clientsWithInvoices = clients.shuffled().take((clients.size * 0.3).toInt())
        
        clientsWithInvoices.forEach { client ->
            val room = roomDao.findById(client.roomId) ?: return@forEach
            
            val totalAmount = room.pricePerDay.multiply(BigDecimal(client.daysReserved))
            
            // Дата выдачи счета - случайная дата в период проживания или после
            val invoiceDate = if (client.checkInDate <= LocalDate.now()) {
                LocalDate.now().minusDays(random.nextInt(0, 30).toLong())
            } else {
                client.checkInDate
            }
            
            val invoice = Invoice(
                invoiceId = 0,
                clientId = client.clientId,
                totalAmount = totalAmount,
                issueDate = invoiceDate
            )
            
            invoiceDao.add(invoice)
        }
    }
}

// Функция для запуска скрипта
fun main() {
    // Подключаемся к базе данных
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/hotel_db",
        driver = "org.postgresql.Driver",
        user = "hotel_admin",
        password = "orf.12014319N"
    )
    
    try {
        println("=".repeat(50))
        println("ЗАПОЛНЕНИЕ БАЗЫ ДАННЫХ ТЕСТОВЫМИ ДАННЫМИ")
        println("=".repeat(50))
        println()
        
        SeedDatabase.seed()
        
        println()
        println("=".repeat(50))
        println("ГОТОВО!")
        println("=".repeat(50))
    } catch (e: Exception) {
        println()
        println("❌ Ошибка при заполнении базы данных: ${e.message}")
        e.printStackTrace()
    }
}

