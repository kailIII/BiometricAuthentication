package biometricauthentication.utils;

import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;

import java.io.File;
import java.io.IOException;

import java.sql.Time;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.text.SimpleDateFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import biometricauthentication.utils.authentication.Information;
import biometricauthentication.utils.hibernate.HibernateUtil;
import biometricauthentication.utils.date.DateUtil;

import biometricauthentication.model.BinnacleRecord;
import biometricauthentication.model.Company;
import biometricauthentication.model.Employee;
import biometricauthentication.model.Shift;
import biometricauthentication.model.Config;
import biometricauthentication.model.EmployeeType;
import biometricauthentication.model.User;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author VakSF
 */
public class Biometric {
    
    private final SessionFactory sessionFactory;
    
    private final DateUtil dateUtil;
    
    private Config configuration;
    
    private Company company;

    public Biometric() {
        
        this.sessionFactory = HibernateUtil.getSessionFactory();
        
        this.dateUtil = new DateUtil();
        
        this.setCompany();
        this.setConfig();
        this.createRoot();
        
    }
    
    private void setCompany() {
        
        String UUID = this.getUUID();
        
        Query query = sessionFactory.openSession().createQuery(
                "FROM Company company WHERE company.UUID = :UUID"
        );
        
        query.setParameter("UUID", UUID);
        
        this.company = (Company) query.setMaxResults(1).uniqueResult();
        
    }
    
    public Company getCompany() {
        return this.company;
    }
    
    private void setConfig() {
        
        Session session = this.sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        Config config = null;
        
        try {
            
            Query query = session.createQuery(
                    "FROM Config config WHERE config.company = :company"
            );
            
            query.setParameter("company", this.company);
            
            config = (Config) query.uniqueResult();            
            
            transaction.commit();
             
        } catch (HibernateException ex) {
              
            if (transaction != null) {
                
                transaction.rollback();
                
            }
             
        } finally {
            
            session.close();
            
        }
        
        this.configuration = config != null ? config : new Config();
        
    }
    
    public Config getConfig() {
        return this.configuration;
    }
    
    public void saveConfiguration(Config config) {
        
        Session session = this.sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        session.saveOrUpdate(config);
        
        transaction.commit();
        session.flush(); session.close();
        
    }
    
    public void saveCompany(Company company) {
        
        Session session = this.sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        session.saveOrUpdate(company);
        
        transaction.commit();
        session.flush(); session.close();
        
    }
    
    public String getUUID() {
        
        String UUID = null;
        
//        try {
//            
//            Process process = Runtime.getRuntime().exec(
//                    "wmic csproduct get uuid"
//            );
//            
//            BufferedReader inputStream = new BufferedReader(
//                    new InputStreamReader(
//                            process.getInputStream()
//                    )
//            );
//            
//            String patternSt = ".+-.+-.+-.+";
//            
//            Pattern pattern = Pattern.compile(patternSt, Pattern.UNICODE_CASE);
//            
//            for (Object line : inputStream.lines().toArray()) {
//                
//                String lineSt = line.toString();
//                
//                while (pattern.matcher(lineSt).find()) {
//                    
//                    UUID = lineSt;
//                    
//                    break;
//                    
//                }
//                
//            }
//            
//        } catch(IOException ex){
//            
//           ex.printStackTrace();
//           
//        }

        
	try {
            
            InetAddress ip = InetAddress.getLocalHost();
            
            System.out.println("Current IP address : " + ip.getHostAddress());
            
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            
            byte[] mac = network.getHardwareAddress();
            
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < mac.length; i++) {
                
                sb.append(
                        String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : "")
                );
                
            }
            
            UUID = sb.toString();
            
            System.out.println("Current MAC address : " + UUID);;
            
	} catch (UnknownHostException ex) {
            
            ex.printStackTrace();
            
	} catch (SocketException ex){
            
            ex.printStackTrace();
            
	}
        
        return UUID;
        
    }
    
    /**
     * Crea la carpeta raíz de Biometric
     */
    private void createRoot() {
        
        File biometricDirectory = new File("C:\\Biometric\\");
        
        if (!biometricDirectory.exists()) {
            
            if (biometricDirectory.mkdir()) {
                
                System.out.println("Root creado: Biometric -> " + biometricDirectory.getAbsolutePath());
                
                File reportDirectory = new File("C:\\Biometric\\Reportes\\");
                
                if (!reportDirectory.exists()) {
                    
                    if (reportDirectory.mkdir()) {
                        
                        System.out.println("Root creado: Reportes -> " + reportDirectory.getAbsolutePath());
                        
                    }
                    
                }
                
            }
            
        } else {
            
            File reportDirectory = new File("C:\\Biometric\\Reportes\\");
                
            if (!reportDirectory.exists()) {

                if (reportDirectory.mkdir()) {

                    System.out.println("Root creado: Reportes -> " + reportDirectory.getAbsolutePath());

                }

            }
            
        }
        
    }
    
    /**
     * Da acceso a un usuario administrador
     * 
     * @param user Nombre de usuario
     * @param password Contraseña de usuario
     * @return La existencia del usuario
     */
    public boolean login(String user, String password) {
        
        /*
        Se obtiene un registro correspondiente
        al usuario y contraseña
         */
        Query query = sessionFactory.openSession().createQuery(
                "FROM User admin WHERE "
                + "admin.user = :user AND "
                + "admin.password = :password"
        );
        
        query.setParameter("user", user);
        query.setParameter("password", password);
        
        User admin = (User) query.setMaxResults(1).uniqueResult();
        
        if (admin != null) {
            
            return admin.getUser().equals(user) 
                    && admin.getPassword().equals(password);
            
        }
        
        return false;
        
    }
    
    /**
     * Guarda un empleado
     * 
     * @param employee es el empleado que se guardará
     * @return 
     * @see         Employee
     */
    public boolean saveEmployee(Employee employee) {
        
        Session session = this.sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
        Se guarda o actualiza el empleado
         */
        Employee employeeFound = this.findEmployee(employee, session);
        
        if (employeeFound == null) {
            
            session.saveOrUpdate(employee);
            
            transaction.commit();
            session.flush(); session.close();
            
            return true;
            
        } else {
            
            return false;
            
        }
        
    }
    
    /**
     * Guarda una huella
     * 
     * @param employee es el empleado que se guardará la huella
     * @see         Employee
     */
    public void saveFingerPrint(Employee employee) {
        
        Session session = this.sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
            Se una huella dactilar
         */
            
        session.saveOrUpdate(employee);

        transaction.commit();
        session.flush(); session.close();
        
    }
    
    /**
     * Elimina un empleado
     * 
     * @param employee es el empleado que se eliminará
     * @see         Employee
     */
    public void deleteEmployee(Employee employee) {
        
        Session session = this.sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
            Se elimina el empleado
        */
        session.delete(employee);
        
        transaction.commit();
        session.flush(); session.close();
        
    }
    
    private Employee findEmployee(Employee employee, Session session) {
        
        int hashCode = employee.hashCode();
        
        System.out.println(hashCode);
        
        Query query = session.createQuery(
                "FROM Employee employee WHERE employee.company = :company AND employee.hash = :hash"
        );
        
        query.setParameter("company", this.company);
        
        query.setParameter("hash", hashCode);
        
        return (Employee) query.setMaxResults(1).uniqueResult();
    }
    
    /**
     * Guarda un registro de bitácora
     * 
     * @param binnacleRecord  es un registro de bitácora
     * @see         BinnacleRecord
     */
    public void saveBinnacleRecord(BinnacleRecord binnacleRecord) {
        
        Session session = this.sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
            Se guarda o actualiza el empleado
        */
        session.saveOrUpdate(binnacleRecord);
        
        transaction.commit();
        session.flush(); session.close();
        
    }
    
    /**
     * Retorna el ultimo registro del empleado que recibe.
     * 
     * @param employee es el empleado que contiene el registro a buscar
     * @return      El último registro del empleado
     * @see         BinnacleRecord
     */
    private BinnacleRecord getLastBinnacleRecord(Employee employee) {
        
        // Se obtiene el ID del empleado
        int employeeId = employee.getId();
        
        Session session = this.sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        BinnacleRecord binnacleRecord = null;
        
        try {
            
            /*
                Se obtienen los registros, se ordenan en orden descendiente y 
                se devuelve un único registro
            */
            binnacleRecord = (BinnacleRecord) session.createQuery(
                    "FROM BinnacleRecord where employee_id = " + employeeId + " order by id desc"
            ).setMaxResults(1).uniqueResult();
            
            transaction.commit();
             
        } catch (HibernateException ex) {
            
            ex.printStackTrace();
              
            if (transaction != null) {
                
                transaction.rollback();
                
            }
             
        } finally {
            
            session.close();
            
        }
        
        return binnacleRecord;
    }
    
    /**
     * Retorna la información del guardado en bitácora.
     * 
     * @param employee especifica a que empleado se le guadará la bitacora
     * @return      informacion del guardado
     * @see         Information
     */
    public Information saveBinnacleRecord(Employee employee) {
        
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        String operation = null, verification = null;
        
        /*
            Se obtiene la fecha actual
        */
        Date currentDate = new Date();
        
        /*
            Se obtiene el último registro del empleado.
        */
        BinnacleRecord lastBinnacleRecord = this.getLastBinnacleRecord(employee);
        
        /*
            En caso que ya exista un registro
        */
        
        if (lastBinnacleRecord != null) {
            
            /*
                Se obtiene el Check In del último registro
            */
            Date checkIn = lastBinnacleRecord.getCheckIn();
            
            /*
                Se comprueba si existe un Check In 
                para evitar algún error en el registro.
                Si no existe, entonces se crea.
            */
            if (checkIn == null) {
                
                verification = this.verifyRange(employee, currentDate, "checkIn");
                
                if (!verification.equals("temprano") && !verification.equals("outOfRange")) {
                    
                    String day = new SimpleDateFormat("EEEE", new Locale("es", "ES")).format(currentDate);
                    
                    lastBinnacleRecord.setCheckIn(currentDate);
                    
                    lastBinnacleRecord.setReport(verification);
            
                    lastBinnacleRecord.setDay(day);
                    
                    /*
                        La operación es de entrada
                    */
                    operation = "Entrada";
                    
                }
            
            /*
                Si existe un Check In, se verifica la
                existencia de un Check Out
            */
            } else {
                
                /*
                    Se obtiene el Check Out del último registro
                */
                Date checkOut = lastBinnacleRecord.getCheckOut();
                
                /*
                    Se comprueba si existe un Check Out
                    Si no existe, se crea.
                */
                if (checkOut == null) {
                    
                    Date cinDate = this.dateUtil.parseSimpleDate(
                            lastBinnacleRecord.getCheckIn(), "date"
                    );
                    
                    Date nowDate = this.dateUtil.parseSimpleDate(
                            currentDate, "date"
                    );
                    
                    Map<TimeUnit, Long> difference = this.dateUtil.getDifference(nowDate, cinDate);
                    
                    String description = employee.getShift().getDescription();
                    
                    int days = difference.get(TimeUnit.DAYS).intValue();
                    
                    if (description.equals("Nocturno")) {
                        
                        if (days < 0) {
                            
                            /* Comprueba si el checkOut puede ser válido */
                            if (days == -1) {
                                
                                verification = this.verifyRange(employee, currentDate, "checkOut");
                                
                                if (!verification.equals("temprano") && !verification.equals("outOfRange")) {
                                    
                                    lastBinnacleRecord.setCheckOut(currentDate);
                                    
                                    /*
                                        La operación es de salida
                                    */
                                    operation = "Salida";
                                    
                                    lastBinnacleRecord = this.setReport(lastBinnacleRecord);
                                    
                                }
                                
                            } else {
                                
                                verification = this.createBinnacleRecord(employee, currentDate);
                                
                                operation = "Entrada";
                                
                            }
                            
                        } else {
                            
                            if (days == 0) {
                                
                                verification = "temprano";
                                
                                operation = "ninguna";
                                
                            }
                            
                        }
                        
                    } else {
                        
                        if (days == 0) {
                            
                            verification = this.verifyRange(employee, currentDate, "checkOut");

                            if (!verification.equals("temprano")) {

                                lastBinnacleRecord.setCheckOut(currentDate);

                                /*
                                    La operación es de salida
                                */
                                operation = "Salida";
                                
                                lastBinnacleRecord = this.setReport(lastBinnacleRecord);
                                
                            }
                            
                        } else {
                            
                            if (days < 0) {
                                
                                verification = this.createBinnacleRecord(employee, currentDate);
                                
                                operation = "Entrada";
                                
                            }
                            
                        }
                        
                        
                    }
                
                /*
                    En caso que ya exista un registro completo
                    se comprobará si la fecha del registro entrante
                    es una fecha posterior a la del último reigstro.
                */
                } else {
                    
                    /*
                        Se obtiene la fecha simple del último registro
                    */
                    Date lastBinnacleRecordDate = this.dateUtil.parseSimpleDate(
                            lastBinnacleRecord.getDate(), "date"
                    );
                    
                    /*
                        Se asigna la fecha simple del registro actual a la fecha actual
                    */
                    currentDate = this.dateUtil.parseSimpleDate(
                            currentDate, "date"
                    );
                    
                    /*
                        Se realiza la comprobación de fechas.
                        
                        Si la fecha entrante es posterior a la del
                        último registro, se creará un nuevo registro.
                    */
                    if (currentDate.after(lastBinnacleRecordDate)) {
                        
                        /*
                            Creación de un nuevo registro.
                        */
                        
                        currentDate = new Date();
                        
                        verification = this.createBinnacleRecord(employee, currentDate);
            
                        operation = "Entrada";
                        
                    /*
                        Si la comprobación de fechas demuestra
                        que las fechas son iguales o la fecha actual
                        es "anterior", se retornará "same_day".
                    */
                    } else {
                        
                        verification = "sameDay";
                        operation = "ninguna";
                        
                    }
                    
                }
                
            }
            
            /*
                Finalmente se realiza una actualización
                a la base de datos.
            */
            
            /* 
                Se obtiene el día de la semana y se establece al registro
            */
            String day = new SimpleDateFormat("EEEE", new Locale("es", "ES")).format(currentDate);
            
            lastBinnacleRecord.setDay(day);
            
            session.saveOrUpdate(lastBinnacleRecord);   
            
        /*
            No existe algún registro del empleado.
            Se realiza la creación de un nuevo registro para el empleado.
        */
        } else {
            
            /*
                Creación de un nuevo registro.
            */
            verification = this.createBinnacleRecord(employee, currentDate);
            
            operation = "Entrada";
            
        }
        
        Information info = new Information(operation, verification);
        
        transaction.commit();
        session.flush(); session.close();
        
        return info;
        
    }
    
    private BinnacleRecord setReport(BinnacleRecord binnacleRecord) {
        
        Time workedHours = this.getWorkedHours(binnacleRecord);
                                
        binnacleRecord.setWorked_hours(workedHours);

        String report = binnacleRecord.getReport();

        if (!report.equalsIgnoreCase("falta")) {

            int maxMinutes =
                    Math.abs(this.configuration.getLateIn()) + Math.abs(this.configuration.getEarlyOut());

            Time maxTime = new Time(7, maxMinutes, 0);

            Map<TimeUnit, Long> workedTime = this.dateUtil.getDifference(workedHours, maxTime);

            int hrs = workedTime.get(TimeUnit.HOURS).intValue();
            int min = workedTime.get(TimeUnit.MINUTES).intValue();

            String action = "";

            System.out.println("hrs = " + hrs);

            if (hrs == 0) {

                if (min > 0) {

                    action = "falta";

                } else {

                    int normalIn = this.configuration.getNormalIn();

                    int absMin = Math.abs(min);

                    if (absMin < normalIn) {

                        action = "retardo";

                    } else {

                        action = "normal";

                    }

                }

            } else {

                if (hrs < 0) {

                    action = "normal";

                }

            }

            if (report.equalsIgnoreCase("retardo")) {

                if (action.equals("falta")) {

                    binnacleRecord.setReport(action);

                }

            } else {

                if (report.equalsIgnoreCase("normal")) {

                    binnacleRecord.setReport(action);
                    
                }

            }

        }
        
        return binnacleRecord;
        
    }
    
    /**
     * Crea un registro en la bitácora.
     * 
     * @param employee especifica a que empleado se le creará la bitacora
     * @param currentDate es una nueva fecha
     * @return      informacion de la operación
     * @see         Employee
     */
    public String createBinnacleRecord(Employee employee, Date currentDate) {
        
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
            Se crea un nuevo registro.
            Se establecen los atributos date, employee_id, checkIn.
        */
        
        String verification = this.verifyRange(employee, currentDate, "checkIn");
        
        if (!verification.equals("temprano") && !verification.equals("outOfRange")) {
                
            Date newDate = new Date();

            BinnacleRecord binnacleRecord = new BinnacleRecord(
                    newDate, employee.getId(), newDate 
            );
            
            /* 
                Se obtiene el día de la semana y se establece al registro
            */
            String day = new SimpleDateFormat("EEEE", new Locale("es", "ES")).format(currentDate);
            
            binnacleRecord.setDay(day);
            
            binnacleRecord.setReport(verification);
            
            /*
                Se inserta el nuevo registro.
            */
            session.save(binnacleRecord);
            
        }
        
        transaction.commit();
        
        session.flush(); session.close();
        
        return verification;
        
    }
    
    /**
     * Verifica un rango de tiempo
     * 
     * @param employee especifica a que empleado se le obtendrá el turno
     * @param currentDate es una nueva fecha
     * @param type es el tipo de operacion (entrada o salida)
     * @return      informacion de la operación
     * @see         Shift
     */
    private String verifyRange(Employee employee, Date currentDate, String type) {
        
        /*
            Se obtiene el turno del empleado
        */
        Shift shift = employee.getShift();
        
        /*
            Se convierte la fecha actual a una fecha de tiempo
        */
        currentDate = this.dateUtil.parseSimpleDate(currentDate, "time");
        
        /*
            Si la operación es de entrada
        */
        if (type.equals("checkIn")) {
            
            /* Se obtiene el checkIn del turno */
            String checkInSt = shift.getCheckIn();
            
            /* Se convierte el checkIn del turno a una fecha de tiempo */
            Date checkIn = this.dateUtil.parseSimpleDateTime(checkInSt, "time");
            
            /*
                Se obtiene una diferencia de tiempo entre 2 fechas de tiempo
            */
            Map<TimeUnit, Long> difference = this.dateUtil.getDifference(checkIn, currentDate);
            
            /*
                Se obtienen las horas y minutos de diferencia
            */
            int hours = this.dateUtil.getHours(difference);
            int minutes = this.dateUtil.getMinutes(difference);
            
            //System.out.println("Horas: " + hours + "\ntoWork: " + shift.getToWork());
            
            if (hours < shift.getToWork()) {
                
                /*
                    Horas y minutos se envían como argumento al CheckIn
                */
                String cin = this.configuration.checkIn(hours, minutes);
                
                return cin;
                
            }
            
        } else {
            
            if (type.equals("checkOut")) {
                
                /* Se obtiene el checkIn del turno */
                String checkOutSt = shift.getCheckOut();
                
                /* Se convierte el checkIn del turno a una fecha de tiempo */
                Date checkOut = this.dateUtil.parseSimpleDateTime(checkOutSt, "time");
                
                /*
                    Se obtiene una diferencia de tiempo entre 2 fechas de tiempo
                */
                Map<TimeUnit, Long> difference = this.dateUtil.getDifference(checkOut, currentDate);
                
                /*
                    Horas y minutos se envían como argumento al CheckIn
                */
                int hours = this.dateUtil.getHours(difference);
                int minutes = this.dateUtil.getMinutes(difference);
                
                /*
                    Horas y minutos se envían como argumento al CheckOut
                */
                String cout = this.configuration.checkOut(hours, minutes);
                
                return cout;
                
            }
            
        }
        
        return "outOfRange";
    }
    
    private Time getWorkedHours(BinnacleRecord br) {
        
        Map<TimeUnit, Long> difference = this.dateUtil.getDifference(
                this.dateUtil.parseSimpleDate(br.getCheckIn(), "time"), 
                this.dateUtil.parseSimpleDate(br.getCheckOut(), "time")
        );
        
        int hours = difference.get(TimeUnit.HOURS).intValue();
        int minutes = difference.get(TimeUnit.MINUTES).intValue();
        
        Time time = new Time(hours, minutes, 0);
        
        return time;
    }
    
    /**
     * Obtiene una lista de turnos
     * 
     * @return      lista de turnos
     * @see         Shift
     */
    public List<Shift> getShifts() {
        
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
            Se crea una lista para almacenar los turnos.
        */
        List<Shift> shifts = new ArrayList<>();
        
        /*
            Se obtienen los turnos existentes.
        */
        try {
            
            shifts = session.createQuery("FROM Shift").list();
            
            transaction.commit();
             
        } catch (HibernateException ex) {
            
            ex.printStackTrace();
              
            if (transaction != null) {
                
                transaction.rollback();
                
            }
             
        } finally {
            
            session.close();
            
        }
        
        return shifts;
        
    }
    
    /**
     * Obtiene una lista de compañias
     * 
     * @return      lista de compañias
     * @see         Company
     */
    public List<Company> getCompanies() {
        
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
            Se crea una lista para almacenar las compañias.
        */
        List<Company> shifts = new ArrayList<>();
        
        /*
            Se obtienen las compañias existentes.
        */
        try {
            
            shifts = session.createQuery("FROM Company").list();
            
            transaction.commit();
             
        } catch (HibernateException ex) {
            
            ex.printStackTrace();
              
            if (transaction != null) {
                
                transaction.rollback();
                
            }
             
        } finally {
            
            session.close();
            
        }
        
        return shifts;
        
    }
    
    /**
     * Obtiene una lista de empleados
     * 
     * @return      lista de empleados
     * @see         Employee
     */
    public List<Employee> getEmployees() {
        
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
            Se crea una lista para almacenar a los empleados.
        */
        List<Employee> employees = new ArrayList<>();
        
        /*
            Se obtienen los empleados existentes.
        */
        try {
            
            Query query = session.createQuery(
                    "FROM Employee employee WHERE employee.company = :company"
            );
            
            query.setParameter("company", this.company);
            
            employees = query.list();
            
            transaction.commit();
             
        } catch (HibernateException ex) {
            
            ex.printStackTrace();
            
            if (transaction != null) {
                
                transaction.rollback();
                
            }
             
        } finally {
            
            session.close();
            
        }
        
        return employees;
        
    }
    
    public List<EmployeeType> getEmployeeTypes() {
        
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
        /*
            Se crea una lista para almacenar a los empleados.
        */
        List<EmployeeType> employees = new ArrayList<>();
        
        /*
            Se obtienen los empleados existentes.
        */
        try {
            
            employees = session.createQuery("FROM EmployeeType").list();
            
            transaction.commit();
             
        } catch (HibernateException ex) {
            
            ex.printStackTrace();
            
            if (transaction != null) {
                
                transaction.rollback();
                
            }
             
        } finally {
            
            session.close();
            
        }
        
        return employees;
        
    }
    
    /**
     * Serializa un template
     * 
     * @param template es un template de una huella dactilar
     * @return      un arreglo de bytes
     * @see         DPFPTemplate
     */
    public byte[] serializeTemplate(DPFPTemplate template) {
        return template.serialize();    
    }
    
    /**
     * Deserializa un template
     * 
     * @param employee es el empleado al que se le obtendrá el template
     * @return      un template de una huella dactilar
     * @see         DPFPTemplate
     */
    public DPFPTemplate deserializeTemplate(Employee employee) {
        
        /*
            Se crea un template vacío.
        */
        DPFPTemplate template = DPFPGlobal.getTemplateFactory().createTemplate();
        
        /*
            Se deserializa el template
        */
        try {
            
            /*
                Se obtienen los bytes del template del empleado.
                Se asigna los deserializado al template anteriormente vacío.
            */
            template.deserialize(employee.getTemplate());
            
        } catch (IllegalArgumentException ex) {
            
            System.out.println("Empleado sin huella: " + employee);
            
            /*
                Si existe algún problema con la deserialización, 
                el template será nulo.
            */
            template = null;
            
        }
        
        return template;
        
    }
    
    /**
     * Guarda un archivo de imagen
     * 
     * @param employee es el empleado al que se le guardará la imagen
     * @param file es la imagen a guardar
     * @return      si fue guardado o no
     * @throws java.io.IOException
     */
    public boolean saveFile(Employee employee, File file) throws IOException {
        
        /*
            Se genera un url para la imagen del empleado.
        */
        String url = "C:\\Biometric\\".concat(String.valueOf(employee.getId())).concat(".png");
        
        /*
            Se establece una instancia de un archivo, 
            el cual será almacenado.
        */
        File toSave = new File(url);
        
        /*
            Si el archivo no existe, se crea el archivo.
        */
        if (!toSave.exists()) toSave.createNewFile();
        
        /*
            Se lee archivo por un BufferedImage
        */
        BufferedImage image = ImageIO.read(file);
        
        /*
            Se escriben los datos del buffer en el archivo
            creado posteriormente.
        */
        boolean written = ImageIO.write(image, "png", toSave);
        
        return written;
    }
    
    /**
     * Obtiene la imagen del empleado
     * 
     * @param employee es el empleado al que se le obtendrá la imagen
     * @return      la imagen del empleado
     */
    public File getFile(Employee employee) {
        
        /*
            Se genera un url para la imagen del empleado.
        */
        String url = "C:\\Biometric\\".concat(String.valueOf(employee.getId())).concat(".png");
        
        /*
            Se establece una instancia de un archivo, 
            el cual será recuperado.
        */
        File file = new File(url);
        
        /*
            Si el archivo existe, se retornará.
        */
        if (file.exists()) {
            
            return file;
        
        /*
            Si no existe, retornará nulo, para mostrar
            la imagen por defecto.
        */
        } else {
            
            return null;
            
        }
        
    }
    
    /**
     * Verifica si un sample coincide con un template
     * 
     * @param sample es la muestra obtenida por el Reader
     * @param template es el modelo extraido del empleado
     * @return      si fue verificado o no
     */
    public boolean verify(DPFPSample sample, DPFPTemplate template)  {
        
        boolean verified = false;
        
        try {
            
            /* Se crea un extractor de características */
            DPFPFeatureExtraction featureExtractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
            
            /* Se crea un set de características a través de la muestra */
            DPFPFeatureSet featureSet = featureExtractor.createFeatureSet(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
            
            /* Se crea un verificador */
            DPFPVerification matcher = DPFPGlobal.getVerificationFactory().createVerification();
            
            /* Se establece la seguridad FAR */
            matcher.setFARRequested(DPFPVerification.MEDIUM_SECURITY_FAR);
            
            /* Se verifica entre el ser de características y el template */
            DPFPVerificationResult result = matcher.verify(featureSet, template);
            
            /* Se obtiene el resultado de la verificación */
            verified = result.isVerified();
                        
        } catch (DPFPImageQualityException ex) {
            
            ex.printStackTrace();
            
        }
        
        return verified;
        
    }
    
}