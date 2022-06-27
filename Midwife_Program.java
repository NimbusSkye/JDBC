import java.sql.* ;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

class GoBabbyApp
{

  public static java.sql.ResultSet getAppointments (Connection con, Statement statement, int practitioner_id) throws SQLException {
    java.sql.ResultSet rs = null;
    String date = null;
    String query = null;
    Scanner s = new Scanner(System.in);
    while (true) {
        System.out.print("Please enter the date (YYYY-MM-DD) for appointment list [E] to exit: ");
        date = s.next();

        if (date.equals("E")) 
          disconExit(con,statement);

        query = "SELECT Appointment.time, Parent.name, Parent.ramqid, \'P\' AS Type, Appointment.id " +
        "FROM Midwife, Appointment, Couple, Mother, Parent, AssignedTo " +
        "WHERE Appointment.practitioner = Midwife.id AND " + 
        "Appointment.coupleid = Couple.id AND " +
        "Couple.mother = Mother.id AND " +
        "Mother.id = Parent.id AND "+
        "AssignedTo.coupleid=Couple.id AND "+
        "AssignedTo.practitioner=Midwife.id AND "+
        "Appointment.practitioner = " +
        practitioner_id +
        " AND Appointment.date = DATE " +
        "\'"+date+"\'"+
        "UNION "+
        "SELECT Appointment.time, Parent.name, Parent.ramqid, \'B\' AS Type, Appointment.id " +
        "FROM Midwife, Appointment, Couple, Mother, Parent, BackupFor " +
        "WHERE Appointment.practitioner = Midwife.id AND " + 
        "Appointment.coupleid = Couple.id AND " +
        "Couple.mother = Mother.id AND " +
        "Mother.id = Parent.id AND "+
        "BackupFor.coupleid=Couple.id AND "+
        "BackupFor.practitioner=Midwife.id AND "+
        "Appointment.practitioner = " +
        practitioner_id +
        " AND Appointment.date = DATE " +
        "\'"+date+"\'" +
        " ORDER BY time"
        ;

        rs = statement.executeQuery(query);

        if(rs.isBeforeFirst()) return rs;

        System.out.println("No appointments on that date.");
      }
  }

  public static void getNotes (int pregnancyID, Statement statement) throws SQLException{
    String query = "SELECT Note.date, Note.time, Note.observations "+
        "FROM Note, Appointment, Pregnancy "+
        "WHERE Note.aptment = Appointment.id "+
        "AND Appointment.pregnancy = Pregnancy.id "+
        "AND Pregnancy.id = "+
        pregnancyID+
        " ORDER BY date DESC, time DESC"; 

    java.sql.ResultSet rs = statement.executeQuery(query);

    if(!rs.isBeforeFirst()) 
    {
      System.out.print("No notes.\n\n");
      return;
    }

    while ( rs.next ( ) )
    {
      System.out.print (rs.getString(1)+"\t");
      System.out.print (rs.getString(2)+"\t");
      String txt = rs.getString(3);
      System.out.println (txt.substring(0, Math.min(txt.length(), 50)));
    }
    System.out.print("\n\n");
  }

  public static void getTests (int pregnancyID, Statement statement) throws SQLException {
    String query = "SELECT Test.prescribedDate, Test.type, Test.result "+
        "FROM Test, Appointment, Pregnancy "+
        "WHERE Test.aptment = Appointment.id "+
        "AND Appointment.pregnancy = Pregnancy.id "+
        "AND Pregnancy.id = "+
        pregnancyID+
        " ORDER BY prescribedDate DESC";

    java.sql.ResultSet rs = statement.executeQuery(query);

    if(!rs.isBeforeFirst()) 
    {
      System.out.print("No tests.\n\n");
      return;
    }

    while ( rs.next ( ) )
    {
      System.out.print (rs.getString(1)+"\t");
      System.out.print ("["+rs.getString(2)+"]"+"\t");
      
      String result = rs.getString(3);
      try {
        result.equals("");
        System.out.println(result.substring(0, Math.min(result.length(), 50)));
      }
      catch (NullPointerException e) {
        System.out.println ("-");
      }
    }
    System.out.print("\n\n");
  }

  public static void addNote (int aptmentID, String observation, Statement statement) throws SQLException {
    LocalDate now = LocalDate.now();
    LocalTime rn = LocalTime.now();

    String insertSQL = "INSERT INTO Note "+ 
      "VALUES ("+
      "\'"+observation+"\'"+","+
      "DATE "+"\'"+now+"\'"+","+
      "TIME "+"\'"+rn.truncatedTo(ChronoUnit.SECONDS)+"\'"+","+
      aptmentID+
      ")"
      ;

    statement.executeUpdate ( insertSQL ) ;
    System.out.print("Note successfully added.\n\n");
  }

  public static void addTest (int aptmentID, String type, Statement statement) throws SQLException {
    int newID = 1;
    java.sql.ResultSet rs = statement.executeQuery("SELECT MAX(id) FROM Test");
    if (rs.next()) 
      newID = rs.getInt(1)+1;

    LocalDate now = LocalDate.now();

    String insertSQL = "INSERT INTO Test (id, prescribedDate, parentOrBaby, type, aptment, technician) "+ 
      "VALUES ("+
      newID+","+
      "DATE "+"\'"+now+"\'"+","+
      "\'P\',"+
      "\'"+type+"\'"+","+
      aptmentID+",1"+
      ")"
      ;

    statement.executeUpdate ( insertSQL ) ;
    System.out.print("Test successfully added.\n\n");
  }

  public static void disconExit (Connection con, Statement statement) throws SQLException {
    System.out.println("Goodbye.");
    statement.close ( ) ;
    con.close ( ) ;
    System.exit(0);
  }

  public static void main ( String [ ] args ) throws SQLException
  {
    ArrayList<Integer> aptments = new ArrayList<>();

    Scanner s = new Scanner(System.in);

    int sqlCode=0;
    String sqlState="00000";

    // Register the driver before using it
    try { DriverManager.registerDriver ( new com.ibm.db2.jcc.DB2Driver() ) ; }
    catch (Exception e){ System.out.println("Class not found"); }

    String url = "DB URL";

    String user_id = "user";
    String pass = "pass";
    
    Connection con = DriverManager.getConnection (url,user_id,pass) ;
    Statement statement = con.createStatement (ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE ) ;
    int practitioner_id=-1;
    String response=null;

    menu1: while(true) 
    {

      System.out.print("Please enter your practitioner id [E] to exit: ");
      response = s.next();

      if (response.equals("E")) {
        System.out.println("Goodbye.");
        System.exit(0);
      }

      try {
        practitioner_id = Integer.parseInt(response);
      }
      catch (Exception e) {
        System.out.println("Not an integer.");
        continue;
      }

      // Query for practitioner ID to check if exists
      try
      {
        String query = "SELECT id FROM Midwife WHERE id=" + practitioner_id;
        
        java.sql.ResultSet rs = statement.executeQuery(query);
        if (!rs.isBeforeFirst() )
          System.out.println("Invalid Practitioner ID."); 
        else
          break menu1;
        

      }
      catch (SQLException e)
      {
        sqlCode = e.getErrorCode(); 
        sqlState = e.getSQLState(); 
              
        // Error handling
        System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
        System.out.println(e);
      }
    }

    // Find all appointments for this midwife on this date
    try
    {
      while(true) 
      {
        java.sql.ResultSet rs = getAppointments(con, statement, practitioner_id);
        String appointments="";

        int counter = 1;

        while ( rs.next ( ) )
          {
            aptments.add(rs.getInt(5)); 
            appointments+=counter+":\t";
            appointments+=rs.getString(1)+"\t";
            appointments+=rs.getString(4)+"\t";
            appointments+=rs.getString(2)+"\t";
            appointments+=rs.getString(3)+"\n";
            counter++;
          }

        while (true) 
        {

          System.out.println(appointments);

          System.out.println("Enter the appointment number that you would like to work on.");
          System.out.print ("[E] to exit [D] to go back to another date : ");
          response = s.next();
          System.out.println();

          if (response.equals("E")) 
            disconExit(con,statement);
          
          if (response.equals("D"))
            break;

          int aptment=0;

          try 
          {
            aptment = Integer.parseInt(response);
          }
          catch(Exception e) 
          {
            System.out.println ("Invalid command. Exiting.");
            disconExit(con,statement);
          }

          String query = "SELECT pregnancy "+
            "FROM Appointment "+
            "WHERE id = "+
            aptments.get(aptment-1);

          rs = statement.executeQuery(query);
          rs.next();
          int pregnancyID = rs.getInt(1);

          query = "SELECT Parent.name,Parent.ramqid "+
            "FROM Pregnancy, Mother, Parent "+
            "WHERE Pregnancy.mother = Mother.id "+
            "AND Mother.id = Parent.id "+
            "AND Pregnancy.id = "+
            pregnancyID;

          rs = statement.executeQuery(query);
          rs.next();

          String prompt = "For "+rs.getString(1)+" "+rs.getString(2)+"\n\n";

          loop: while(true) 
          {
            System.out.print (prompt);
            System.out.print ("1. Review notes\n2. Review tests\n3. Add a note\n4. Prescribe a test\n5. Go back to the appointments.\n\n");
            System.out.print ("Enter your choice: ");
            response=s.next();
            System.out.println();
            switch(response) {
              case "1":
                getNotes(pregnancyID, statement);
                break;
              case "2":
                getTests(pregnancyID, statement);
                break;
              case "3":
                System.out.print ("Please type your observation: ");
                s.nextLine();
                response = s.nextLine();
                addNote(aptments.get(aptment-1),response,statement);
                break;
              case "4":
                System.out.print ("Please enter the type of test: ");
                s.nextLine();
                response = s.nextLine();
                addTest(aptments.get(aptment-1),response,statement);
                break;
              default:
                break loop;
            }
          }
        }
        aptments.clear();
      }
    }

    catch (SQLException e)
    {
      sqlCode = e.getErrorCode(); 
      sqlState = e.getSQLState(); 
            
      // Error handling
      System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
      System.out.println(e);
    }
  }
}
