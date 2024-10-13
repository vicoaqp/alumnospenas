class Attendance(
    val nombres: String,
    val grado: String,
    val timestamp: Timestamp,
    val seccion: String,
    val dni: String,
    val tipo: String
)

class AttendanceAdapter : ListAdapter<Attendance, AttendanceAdapter.AttendanceViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Attendance>() {
            override fun areItemsTheSame(oldItem: Attendance, newItem: Attendance): Boolean {
                // Compara los elementos por su DNI (o cualquier ID único)
                return oldItem.dni == newItem.dni
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Attendance, newItem: Attendance): Boolean {
                // Compara todos los campos para ver si el contenido ha cambiado
                return oldItem == newItem
            }
        }
    }

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvHora: TextView = itemView.findViewById(R.id.tvHora)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val attendance = getItem(position)

// Asegurarse de que el timestamp se convierta a un Date antes de formatearlo
        val formattedDate = attendance.timestamp?.toDate()?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
        } ?: "Fecha no disponible"

        val formattedTime = attendance.timestamp?.toDate()?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        } ?: "Hora no disponible"

        holder.tvFecha.text = "Fecha: $formattedDate"
        holder.tvHora.text = "Hora: $formattedTime"
        holder.tvTipo.text = "Tipo: ${attendance.tipo}"
    }
}

data class Event(
    val title: String = "",
    val description: String = "",
    val imageUrl: String = ""
)

class EventsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var eventsAdapter: EventsAdapter
    private val eventsList = mutableListOf<Event>()
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        // Suscribir al tema general de eventos
        // Suscribir al tema general de eventos
        FirebaseMessaging.getInstance().subscribeToTopic("general_events")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Suscrito a notificaciones de eventos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al suscribirse a notificaciones", Toast.LENGTH_SHORT).show()
                }
            }


        // Recuperar el tipo de usuario desde SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        isAdmin = sharedPreferences.getString("userType", "alumno") == "administrador"

        // Inicializar Firestore y RecyclerView
        db = FirebaseFirestore.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(this)

        eventsAdapter = EventsAdapter(eventsList)
        recyclerView.adapter = eventsAdapter

        // Cargar los eventos desde Firestore
        fetchEventsFromFirebase()

        // Configurar el BottomNavigationView basado en el rol del usuario
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        setupBottomNavigationView(bottomNavigationView)



    }

    private fun setupBottomNavigationView(bottomNavigationView: BottomNavigationView) {
        // Configurar el menú del BottomNavigationView según el rol del usuario
        bottomNavigationView.menu.clear() // Limpiar el menú existente

        if (isAdmin) {
            // Si es administrador, mostrar las opciones "Escanear" y "Salir"
            bottomNavigationView.inflateMenu(R.menu.menu_admin)
        } else {
            // Si es alumno, mostrar las opciones "Asistencias" y "Salir"
            bottomNavigationView.inflateMenu(R.menu.menu_student)
        }

        // Configurar el listener para manejar las selecciones del menú
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            handleBottomNavigation(menuItem)
            true
        }
    }

    private fun fetchEventsFromFirebase() {
        db.collection("events")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("title") ?: ""
                    val description = document.getString("description") ?: ""
                    val imageUrl = document.getString("imageUrl") ?: ""

                    val event = Event(title, description, imageUrl)
                    eventsList.add(event)
                }
                eventsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar eventos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun handleBottomNavigation(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_assistances -> {
                // Navegar a la pantalla de asistencias (Record) si el alumno selecciona "Asistencias"
                startActivity(Intent(this,record::class.java))
            }
            R.id.action_scan -> {
                // Iniciar el escaneo de QR para ingresar la asistencia si es administrador
                scanQRCode()
            }
            R.id.action_logout -> {
                // Cerrar sesión para cualquier usuario
                logoutUser()
            }
        }
        return true
    }

    private fun scanQRCode() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanear código QR")
        integrator.setCameraId(0) // Cámara trasera
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val dni = result.contents  // DNI extraído del QR
                registerAttendance(dni)
                //Toast.makeText(this, dni, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    private fun registerAttendance(dni: String) {
        val db = FirebaseFirestore.getInstance()

        // Obtener la marca de tiempo actual
        val currentTimestamp = Date()

        // Buscar el último registro de asistencia del estudiante por su DNI y fecha actual
        db.collection("attendances")
            .whereEqualTo("dni", dni)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Ordenar por la marca de tiempo más reciente
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                var tipoAsistencia = "Entrada" // Valor predeterminado

                // Si existe un registro de asistencia en la fecha actual, alternar el tipo
                if (!documents.isEmpty) {
                    val lastAttendance = documents.first()
                    val lastTipo = lastAttendance.getString("tipo") ?: "Salida"

                    // Alternar entre "Entrada" y "Salida"
                    tipoAsistencia = if (lastTipo == "Entrada") "Salida" else "Entrada"
                }

                // Buscar al estudiante por su DNI para obtener más detalles
                db.collection("students").whereEqualTo("dni", dni)
                    .get()
                    .addOnSuccessListener { studentDocuments ->
                        if (studentDocuments.isEmpty) {
                            Toast.makeText(this, "Estudiante no encontrado", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        for (document in studentDocuments) {
                            val student = document.toObject(Student::class.java)

                            // Crear un registro de asistencia
                            val attendance = hashMapOf(
                                "nombres" to student.nombres,
                                "dni" to student.dni,
                                "grado" to student.grado,
                                "seccion" to student.seccion,
                                "timestamp" to currentTimestamp, // Almacenar como Timestamp
                                "tipo" to tipoAsistencia
                            )

                            // Registrar la asistencia en la colección "attendances"
                            db.collection("attendances")
                                .add(attendance)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Asistencia registrada como $tipoAsistencia correctamente", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al registrar la asistencia", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Error al buscar estudiante", e)
                        Toast.makeText(this, "Error al buscar estudiante: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error al buscar último registro de asistencia", e)
                Toast.makeText(this, "Error al verificar el último registro de asistencia: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutUser() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }
}

class EventsAdapter(private val eventsList: List<Event>) : RecyclerView.Adapter<EventsAdapter.EventsViewHolder>() {

    class EventsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventImage: ImageView = itemView.findViewById(R.id.eventImage)
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        val eventDescription: TextView = itemView.findViewById(R.id.eventDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventsViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventsViewHolder, position: Int) {
        val event = eventsList[position]
        holder.eventTitle.text = event.title
        holder.eventDescription.text = event.description
        Glide.with(holder.itemView.context).load(event.imageUrl).into(holder.eventImage)
    }

    override fun getItemCount(): Int = eventsList.size
}

class Login : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        FirebaseMessaging.getInstance().subscribeToTopic("general_events")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Suscripción exitosa al tema general_events")
                } else {
                    Log.e("FCM", "Error al suscribirse al tema general_events", task.exception)
                }
            }

        // Comprobar si el usuario ya ha iniciado sesión
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)


        if (isLoggedIn) {
            val dni = sharedPreferences.getString("dni", "")
            val userType = sharedPreferences.getString("userType", "alumno")

            // Redirigir según el tipo de usuario
            if (userType != null) {
                redirectToActivity(userType, dni)
            }
            return
        }


        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        val correo = findViewById<EditText>(R.id.editTextEmail)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val btnRegister = findViewById<Button>(R.id.buttonRegister)

        btnLogin.setOnClickListener {
            val correoText = correo.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (correoText.isNotEmpty() && passwordText.isNotEmpty()) {
                loginUser(correoText, passwordText)
            } else {
                Toast.makeText(this, "Por favor ingrese el correo y la contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, register::class.java)
            startActivity(intent)
        }

    }

    private fun loginUser(correo: String, password: String) {
        db.collection("students")
            .whereEqualTo("correo", correo)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in documents) {
                        val dni = document.getString("dni") ?: ""
                        val userType = document.getString("tipo") ?: "alumno"

                        // Guardar el estado de inicio de sesión en SharedPreferences
                        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("dni", dni)
                        editor.putString("userType", userType)
                        editor.apply()

                        // Redirigir según el tipo de usuario
                        redirectToActivity(userType, dni)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al iniciar sesión: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun redirectToActivity(userType: String, dni: String?) {
        val intent = if (userType == "administrador") {
            Intent(this, EventsActivity::class.java)
        } else {
            Intent(this, record::class.java)
        }
        intent.putExtra("dni", dni)
        startActivity(intent)
        finish()
    }

}

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Maneja el mensaje cuando la aplicación está en primer plano

        // Si el mensaje tiene datos (data payload), procesarlos
        if (remoteMessage.data.isNotEmpty()) {
            handleDataPayload(remoteMessage.data)
        }

        // Si el mensaje tiene una notificación (notification payload), procesar y mostrar
        remoteMessage.notification?.let {
            val title = it.title ?: "Notificación"
            val body = it.body ?: "Tienes una nueva notificación"
            sendNotification(title, body)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        // Procesar el payload de datos cuando está en primer plano
        val title = data["title"] ?: "Notificación"
        val body = data["body"] ?: "Tienes una nueva notificación"
        sendNotification(title, body)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Canal de Notificaciones",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Token refrescado: $token")
        // Envía el token al backend si es necesario
    }
}

class record : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private var isAdmin = false
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    // Referencia al TextView del mes seleccionado
    private lateinit var tvSelectedMonth: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        try {
            // Recuperar el tipo de usuario desde SharedPreferences
            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            isAdmin = sharedPreferences.getString("userType", "alumno") == "administrador"

            // Inicializar Firestore
            db = FirebaseFirestore.getInstance()

            // Inicializar RecyclerView
            recyclerView = findViewById(R.id.rvAttendances)
            recyclerView.layoutManager = LinearLayoutManager(this)

            // Configurar el adaptador
            attendanceAdapter = AttendanceAdapter()
            recyclerView.adapter = attendanceAdapter


            // Obtener el TextView que muestra el mes seleccionado
            tvSelectedMonth = findViewById(R.id.tvSelectedMonth)

            // Obtener el DNI del alumno logueado desde SharedPreferences
            val dni = sharedPreferences.getString("dni", null)

            if (dni == null || dni.isEmpty()) {
                Toast.makeText(this, "Error al obtener el DNI", Toast.LENGTH_SHORT).show()
                Log.e("RecordActivity", "DNI no encontrado en SharedPreferences")


            } else {
                Toast.makeText(this, "El DNI es $dni", Toast.LENGTH_SHORT).show()
                // Actualizar el texto del mes seleccionado por defecto (mes y año actuales)
                updateSelectedMonthText()
                fetchAttendanceRecords(dni, selectedMonth, selectedYear)
            }

            // Configurar BottomNavigationView
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
                handleBottomNavigation(menuItem)
                true
            }

            if (!isAdmin) {
                bottomNavigationView.menu.findItem(R.id.action_scan)?.isVisible = false
            }
            // Agregar listener al botón para seleccionar otro mes
            val btnSelectMonth = findViewById<Button>(R.id.btnSelectMonth)
            btnSelectMonth.setOnClickListener {
                if (dni != null) {
                    showMonthPickerDialog(dni) // Mostrar el diálogo de selección de mes
                }
            }

        } catch (e: Exception) {
            Log.e("RecordActivity", "Error durante la inicialización de la actividad", e)
            Toast.makeText(this, "Ocurrió un error al inicializar la pantalla: ${e.message}", Toast.LENGTH_LONG).show()
        }

    }

    private fun showMonthPickerDialog(dni: String) {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, _ ->
                // Actualizar el mes y año seleccionado
                selectedMonth = month
                selectedYear = year
                // Actualizar el texto del mes seleccionado
                updateSelectedMonthText()

                fetchAttendanceRecords(dni, selectedMonth, selectedYear)
            },
            selectedYear,
            selectedMonth,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.findViewById<View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = View.GONE // Ocultar selección de días
        dialog.show()
    }

    private fun updateSelectedMonthText() {
        // Convertir el mes y año seleccionados a un formato legible
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, selectedMonth)
        calendar.set(Calendar.YEAR, selectedYear)

        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        // Actualizar el TextView con el mes seleccionado
        tvSelectedMonth.text = "Mostrando asistencia de: $formattedDate"
    }
    // Función para manejar las selecciones del menú inferior
    private fun handleBottomNavigation(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_events -> {
                startActivity(Intent(this, EventsActivity::class.java))
            }
            R.id.action_logout -> {
                logoutUser()
            }
        }
    }


    /// Función para obtener los registros de asistencia del mes y año seleccionados
    private fun fetchAttendanceRecords(dni: String, month: Int, year: Int) {
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startDate = calendar.time // Primer día del mes seleccionado

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endDate = calendar.time // Último día del mes seleccionado

            db.collection("attendances")
                .whereEqualTo("dni", dni)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val newAttendanceList = mutableListOf<Attendance>()
                    for (document in documents) {
                        val nombres = document.getString("nombres") ?: ""
                        val grado = document.getString("grado") ?: ""
                        val seccion = document.getString("seccion") ?: ""
                        val timestamp = document.getTimestamp("timestamp")
                        val dni = document.getString("dni") ?: ""
                        val tipo = document.getString("tipo") ?: ""

                        val attendance = Attendance(nombres, grado, timestamp ?: Timestamp.now(), seccion, dni, tipo)
                        newAttendanceList.add(attendance)
                    }
                    attendanceAdapter.submitList(newAttendanceList)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error al obtener los registros: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FirestoreError", "Error al obtener los registros de asistencia", exception)
                }
        } catch (e: Exception) {
            Log.e("RecordActivity", "Error al buscar registros de asistencia", e)
            Toast.makeText(this, "Ocurrió un error al buscar registros de asistencia: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun logoutUser() {
        // Borrar el estado de inicio de sesión en SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Redirigir al Login Activity
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

}

class register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val apellidos = findViewById<EditText>(R.id.editTextLastName)
        val correo = findViewById<EditText>(R.id.editTextEmail)
        val dni = findViewById<EditText>(R.id.editTextDni)
        val grado = findViewById<EditText>(R.id.editTextGrado)
        val nombres = findViewById<EditText>(R.id.editTextName)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val seccion = findViewById<EditText>(R.id.editTextLastSeccion)
        val btnRegistrar = findViewById<Button>(R.id.buttonRegister)


        btnRegistrar.setOnClickListener {
            val apellidosText = apellidos.text.toString().trim()
            val correoText = correo.text.toString().trim()
            val dniText = dni.text.toString().trim()
            val gradoText = grado.text.toString().trim()
            val nombresText = nombres.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val seccionText = seccion.text.toString().trim()

            if (apellidosText.isNotEmpty() && correoText.isNotEmpty() && dniText.isNotEmpty() &&
                gradoText.isNotEmpty() && nombresText.isNotEmpty() && passwordText.isNotEmpty() && seccionText.isNotEmpty()) {

                // Verificar si el DNI ya está registrado
                checkDniExists(dniText) { exists ->
                    if (exists) {
                        // Mostrar mensaje de error si el DNI ya está registrado
                        Toast.makeText(this, "El DNI ya está registrado", Toast.LENGTH_SHORT).show()
                    } else {
                        // Proceder con el registro si el DNI no existe
                        registerUser(apellidosText, correoText, dniText, gradoText, nombresText, passwordText, seccionText)
                    }
                }
            } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

    }


    // Verificar si el DNI ya existe en la colección "students"
    private fun checkDniExists(dni: String, callback: (Boolean) -> Unit) {
        db.collection("students").document(dni)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // El documento con el DNI ya existe
                    callback(true)
                } else {
                    // El documento no existe, el DNI está disponible
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                // Manejo de error al consultar Firestore
                Toast.makeText(this, "Error al verificar el DNI: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    private fun registerUser(apellidos: String, correo: String, dni: String, grado: String, nombres: String, password: String, seccion: String) {
        val student = hashMapOf(
            "apellidos" to apellidos,
            "correo" to correo,
            "dni" to dni,
            "grado" to grado,
            "nombres" to nombres,
            "password" to password,
            "seccion" to seccion,
            "tipo" to "alumno" // Tipo predeterminado al registrarse
        )


        // Agregar los datos a la colección "students"
        db.collection("students").document(dni).set(student)
            .addOnSuccessListener {
                Toast.makeText(this, "Alumno registrado con éxito", Toast.LENGTH_SHORT).show()
                finish() // Cerrar la actividad después de registrar al alumno
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al registrar al alumno: ${e.message}", Toast.LENGTH_SHORT).show()
            }


    }


}

data class Student(
    val apellidos: String="",
    val dni: String = "",
    val grado: String = "",
    val nombres: String = "",
    val password: String = "",
    val seccion: String = "",
    val correo: String = ""
)


