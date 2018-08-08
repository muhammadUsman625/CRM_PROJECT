package JCode.mysql;

import JCode.CommonTasks;
import JCode.emailControl;
import objects.*;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EmailQueries {

    private Connection static_con;
    private ESetting eSetting;
    private Users user;
    private EmailPhoneQueries emailPhoneQueries;

    public EmailQueries(Connection static_con, Users user, ESetting eSetting, EmailPhoneQueries emailPhoneQueries) {
        this.static_con = static_con;
        this.user = user;
        this.eSetting = eSetting;
        this.emailPhoneQueries = emailPhoneQueries;
    }

    public Users getNoOfSolvedEmails(Users user) {
        String query = " SELECT COUNT(EMNO) AS EMNO FROM EMAIL_STORE " +
                " WHERE SOLVBY = ?";

//        // Connection con = getConnection();
        PreparedStatement statement = null;
        ResultSet set = null;

        try {
            statement = static_con.prepareStatement(query);
            statement.setInt(1, user.getUCODE());
            set = statement.executeQuery();

            while (set.next()) {
                user.setSolved(set.getInt("EMNO"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }

    public void createEmailRelations(Email email) {
        try {
            for (Address address : email.getFromAddress()) {
                System.out.println("In from");
                subCreateEmailRelation(address, email);
            }
            if (email.getCcAddress() != null) {
                for (Address address : email.getCcAddress()) {
                    System.out.println("in Cc");
                    subCreateEmailRelation(address, email);
                }
            }
        } catch (SQLException e) {
            e.getLocalizedMessage();
        }
    }

    private String mainQuery = "SELECT DISTINCT EM_ID, EM_NAME, CL_ID, CS_ID, UCODE FROM EMAIL_LIST WHERE EM_NAME LIKE ";
    private String relQuery = "INSERT INTO EMAIL_RELATION (EMNO, EM_ID, EMTYPE, CL_ID, UCODE, CS_ID) VALUES (?, ?, ?, ?, ?, ?)";

    private void subCreateEmailRelation(Address address, Email email) throws SQLException {
        if (address != null) {
            String splitted = "";
            try {
                splitted = address.toString().split("\\<")[1];
                splitted = splitted.split("\\>")[0];    //Split email into only the bare minimum to scan
            } catch (Exception e) {
                System.out.println(e);
                splitted = address.toString();
            }
            System.out.println("Email: " + splitted);
            PreparedStatement statement = static_con.prepareStatement(mainQuery + " '%" + splitted + "%'");
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                int emno = email.getEmailNo(),
                        emid = set.getInt("EM_ID"),
                        cl = set.getInt("CL_ID"),
                        cs = set.getInt("CS_ID"),
                        ucode = set.getInt("UCODE");

                if (cl == 0 && cs == 0 && ucode == 0)   //Unrelated Email
                    continue;

                PreparedStatement stmnt = static_con.prepareStatement(relQuery);
                stmnt.setInt(1, emno);
                stmnt.setInt(2, emid);
                stmnt.setInt(3, 1);
                stmnt.setInt(4, cl);
                stmnt.setInt(5, ucode);
                stmnt.setInt(6, cs);
                stmnt.executeUpdate();

            }
        }
    }

    public List<ContactProperty> getEmailContactRelations(Email email) {
        String query = "SELECT DISTINCT CS.CS_ID, CS_FNAME, CS_LNAME " +
                "FROM CONTACT_STORE as CS, EMAIL_RELATION as ER " +
                "WHERE CS.CS_ID = ER.CS_ID " +
                "AND ER.EMNO = ?";

        PreparedStatement statement = null;
        List<ContactProperty> contacts = new ArrayList<>();
        try {
            statement = static_con.prepareStatement(query);
            statement.setInt(1, email.getEmailNo());
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                ContactProperty c = new ContactProperty();
                c.setCode(set.getInt("CS_ID"));
                c.setFirstName(set.getString("CS_FNAME"));
                c.setLastName(set.getString("CS_LNAME"));
                contacts.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return contacts;
    }

    public List<ClientProperty> getEmailClientRelations(Email email) {
        String query = "SELECT DISTINCT CS.CL_ID, CL_NAME " +
                " FROM CLIENT_STORE AS CS, EMAIL_RELATION as ER " +
                " WHERE CS.CL_ID = ER.CL_ID " +
                " AND ER.CL_ID != 0 " +
                " AND ER.EMNO = ?";

        PreparedStatement statement = null;
        List<ClientProperty> clients = new ArrayList<>();
        try {
            statement = static_con.prepareStatement(query);
            statement.setInt(1, email.getEmailNo());
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                ClientProperty c = new ClientProperty();
                c.setCode(set.getInt("CL_ID"));
                c.setName(set.getString("CL_NAME"));
                clients.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return clients;
    }

    public int getNoOfUnsolved() {
        String query = " SELECT COUNT(EMNO) AS EMNO FROM EMAIL_STORE " +
                " WHERE ESOLV != 'S' AND FREZE = 0";

//        // Connection con = getConnection();
        PreparedStatement statement = null;
        ResultSet set = null;

        try {
            statement = static_con.prepareStatement(query);
            set = statement.executeQuery();

            while (set.next()) {
                return set.getInt("EMNO");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int getNoOfUnlocked() {
        String query = " SELECT COUNT(EMNO) AS EMNO FROM EMAIL_STORE " +
                " WHERE LOCKD = 0 AND ESOLV != 'S' AND FREZE = 0";

//        // Connection con = getConnection();
        PreparedStatement statement = null;
        ResultSet set = null;

        try {
            statement = static_con.prepareStatement(query);
            set = statement.executeQuery();

            while (set.next()) {
                return set.getInt("EMNO");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public void insertEmail(Email email, Message message) {

//        if (checkForRelatedEmails(email))
//            return;

        String query = "INSERT INTO email_store(EMNO,SBJCT,TOADD,FRADD,TSTMP,EBODY,ATTCH,CCADD,ESOLV,MSGNO,LOCKD," +
                "FREZE) SELECT IFNULL(max(EMNO),0)+1,?,?,?,?,?,?,?,?,?,?,? from EMAIL_STORE";

        PreparedStatement statement = null;

        try {
            statement = static_con.prepareStatement(query);
            statement.setString(1, email.getSubject());
            statement.setString(2, email.getToAddressString());
            statement.setString(3, email.getFromAddressString());
            statement.setString(4, email.getTimestamp());
            statement.setString(5, email.getBody());
            statement.setString(6, email.getAttch());
            statement.setString(7, email.getCcAddressString());
            statement.setString(8, String.valueOf(email.getSolvFlag()));
            statement.setInt(9, email.getMsgNo());
            statement.setInt(10, email.getLockd());
            statement.setBoolean(11, email.isFreze());
            statement.executeUpdate();

            statement.close();

            int emno = getEmailNo(email);
            email.setEmailNo(emno);

            createEmailRelations(email);

            if (eSetting.isAuto())
                autoReply(email, message);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void insertEmailManual(Email email) {

        String query = "INSERT INTO email_store(EMNO,SBJCT,TOADD,FRADD,TSTMP,EBODY,ATTCH,CCADD,ESOLV,MSGNO,LOCKD," +
                "FREZE,MANUAL) SELECT IFNULL(max(EMNO),0)+1,?,?,?,?,?,?,?,?,?,?,?,? from EMAIL_STORE";

        PreparedStatement statement = null;

        try {
            statement = static_con.prepareStatement(query);
            statement.setString(1, email.getSubject());
            statement.setString(2, email.getToAddressString());
            statement.setString(3, email.getFromAddressString());
            statement.setString(4, email.getTimestamp());
            statement.setString(5, email.getBody());
            statement.setString(6, email.getAttch());
            statement.setString(7, email.getCcAddressString());
            statement.setString(8, "N");
            statement.setInt(9, email.getMsgNo());
            statement.setInt(10, email.getLockd());
            statement.setBoolean(11, email.isFreze());
            statement.setBoolean(12, true);
            statement.executeUpdate();

            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    //Store email tickets related
    public boolean checkForRelatedEmails(Email email) {
        List<Integer> ints = checkIfEmailIsRelated(email.getSubject());

        if (ints == null || ints.size() == 0)
            return false;
        else {
            for (int i : ints) {
                email.setEmailNo(i);
                insertEmailRelated(email);
                System.out.println("Attached to: \n\t\t" + email);
            }
            return true;
        }
    }

    private List<Integer> checkIfEmailIsRelated(String subject) {

        String query = "SELECT EMNO FROM EMAIL_STORE " +
                " WHERE SBJCT LIKE '%" + subject.trim() + "%' AND ESOLV = 'N' AND FREZE = 0 ";

        PreparedStatement statement = null;

        try {
            statement = static_con.prepareStatement(query);
            ResultSet set = statement.executeQuery();

            if (!set.isBeforeFirst()) {
                return null;
            }

            List<Integer> list = new ArrayList<>();

            while (set.next()) {
                System.out.println("Attaching to: " + set.getInt("EMNO"));
                list.add(set.getInt("EMNO"));
            }

            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void insertEmailRelated(Email email) {

        String query = "INSERT INTO email_store_related(ESR_ID,EMNO,SBJCT,TOADD,FRADD,TSTMP,EBODY,ATTCH,CCADD,MSGNO)" +
                " SELECT IFNULL(max(ESR_ID),0)+1,?,?,?,?,?,?,?,?,? from email_store_related";

        PreparedStatement statement = null;

        try {
            statement = static_con.prepareStatement(query);
            statement.setInt(1, email.getEmailNo());
            statement.setString(2, email.getSubject());
            statement.setString(3, email.getToAddressString());
            statement.setString(4, email.getFromAddressString());
            statement.setString(5, email.getTimestamp());
            statement.setString(6, email.getBody());
            statement.setString(7, email.getAttch());
            statement.setString(8, email.getCcAddressString());
            statement.setInt(9, email.getMsgNo());
            statement.executeUpdate();

            statement.close();

//            int emno = getEmailNo(email);
//            email.setEmailNo(emno);

//            createEmailRelations(email);

//            if (eSetting.isAuto())
//                autoReply(email, message);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private List<Email> readRelatedEmails(Email where) {     //Emails related to every Emails

        String query = "SELECT EMNO, MSGNO, SBJCT, FRADD, TOADD, CCADD, TSTMP, " +
                " EBODY, ATTCH FROM EMAIL_STORE_RELATED WHERE EMNO = ? ";

        List<Email> allEmails = new ArrayList<>();

        try {
            PreparedStatement statement = static_con.prepareStatement(query);
            statement.setInt(1, where.getEmailNo());
            ResultSet set = statement.executeQuery();
            //-------------Creating Email-------------
            while (set.next()) {
                Email email = new Email();
                email.setEmailNo(set.getInt("EMNO"));
                email.setMsgNo(set.getInt("MSGNO"));
                email.setSubject(set.getString("SBJCT"));
                email.setTimestamp(set.getString("TSTMP"));
                email.setTimeFormatted(CommonTasks.getTimeFormatted(email.getTimestamp()));

                email.setBody(set.getString("EBODY"));
                email.setAttch(set.getString("ATTCH"));
                //------From Address
                String[] from = set.getString("FRADD").split("\\^");
                Address[] fromAddress = new Address[from.length];
                for (int i = 1, j = 0; i < from.length; i++, j++) {
                    try {
                        fromAddress[j] = new InternetAddress(from[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setFromAddress(fromAddress);

                //-----To Address
                String[] to = set.getString("TOADD").split("\\^");
                Address[] toAddress = new Address[to.length];
                for (int i = 1, j = 0; i < to.length; i++, j++) {
                    try {
                        toAddress[j] = new InternetAddress(to[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setToAddress(toAddress);

                //----- CC Address
                String[] cc = set.getString("CCADD").split("\\^");
                Address[] ccAddress = new Address[cc.length];
                for (int i = 1, j = 0; i < cc.length; i++, j++) {
                    try {
                        ccAddress[j] = new InternetAddress(cc[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setCcAddress(ccAddress);

                allEmails.add(email);
            }

            // doRelease(con);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        return allEmails;
    }


    public static String autoReplySubject = "Burhani Customer Support - Ticket Number: ";

    private void autoReply(Email email, Message message) {

        String body = "The Ticket Number Issued to you is: " + email.getEmailNo() + "\n" + eSetting.getAutotext();

        Email e = new Email();
        e.setSubject(autoReplySubject + email.getEmailNo());
        e.setToAddress(new Address[]{email.getFromAddress()[0]});
        e.setBody(body + "\n\n\n" + "--------In Reply To--------" + "\n\nSubject:   " + email.getSubject() + "\n\n" + email.getBody());

        emailControl.sendEmail(e, message);

    }

    private int getEmailNo(Email email) {
        String queryEMNO = "SELECT emno FROM email_store" +
                " WHERE msgno = ?" +
                " AND sbjct = ? " +
                " AND tstmp = ?";

        try {

            PreparedStatement statementEMNO = static_con.prepareStatement(queryEMNO);
            statementEMNO.setInt(1, email.getMsgNo());
            statementEMNO.setString(2, email.getSubject());
            statementEMNO.setString(3, email.getTimestamp());
            ResultSet set = statementEMNO.executeQuery();

            int emno = 0;
            //" Mr. " + RecieveName + "\n" +
            while (set.next()) {
                emno = set.getInt(1);
            }

            statementEMNO.close();
            set.close();

            return emno;

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // doRelease(con);
        }

        return 0;
    }

    //Reading tickets
    public List<Email> readAllEmails(Filters filters, UserQueries userQueries) {

        String query = "SELECT EMNO, MSGNO, SBJCT, FRADD, TOADD, CCADD, TSTMP, " +
                " EBODY, ATTCH, ESOLV, LOCKD, SOLVBY, SOLVTIME FROM EMAIL_STORE";

        if (filters == null) {
            query = query + " ORDER BY EMNO DESC";
        } else {
            query = query + " WHERE " + filters.toString();
        }

        List<Email> allEmails = new ArrayList<>();

        try {
            // Connection con = getConnection();
            PreparedStatement statement = static_con.prepareStatement(query);
            System.out.println(query);
            ResultSet set = statement.executeQuery();
            //-------------Creating Email-------------
            if (!set.isBeforeFirst()) {
                return null;
            }

            while (set.next()) {
                Email email = new Email();
                email.setEmailNo(set.getInt("EMNO"));
                email.setMsgNo(set.getInt("MSGNO"));
                email.setSubject(set.getString("SBJCT"));
                email.setTimestamp(set.getString("TSTMP"));
                email.setTimeFormatted(CommonTasks.getTimeFormatted(email.getTimestamp()));

                email.setBody(set.getString("EBODY"));
                email.setAttch(set.getString("ATTCH"));
                email.setSolvFlag(set.getString("ESOLV").charAt(0));
                email.setLockd(set.getInt("LOCKD"));
                if (set.getInt("LOCKD") == '\0') {
                    email.setLockedByName("Unlocked");
                } else {
                    email.setLockedByName(userQueries.getUserName(email.getLockd())); //Getting name of username that
                    // locked
                }                                                              // particular email

                //------From Address
                String[] from = set.getString("FRADD").split("\\^");
                Address[] fromAddress = new Address[from.length];
                for (int i = 1, j = 0; i < from.length; i++, j++) {
                    try {
                        fromAddress[j] = new InternetAddress(from[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setFromAddress(fromAddress);

                //-----To Address
                String[] to = set.getString("TOADD").split("\\^");
                Address[] toAddress = new Address[to.length];
                for (int i = 1, j = 0; i < to.length; i++, j++) {
                    try {
                        toAddress[j] = new InternetAddress(to[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setToAddress(toAddress);

                //----- CC Address
                String[] cc = set.getString("CCADD").split("\\^");
                Address[] ccAddress = new Address[cc.length];
                for (int i = 1, j = 0; i < cc.length; i++, j++) {
                    try {
                        ccAddress[j] = new InternetAddress(cc[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setCcAddress(ccAddress);

                email.setRelatedContacts(getEmailContactRelations(email));
                email.setRelatedClients(getEmailClientRelations(email));

                email.setRelatedEmails(readRelatedEmails(email));

                allEmails.add(email);
            }

            // doRelease(con);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return allEmails;
    }


    public void insertEmailGeneral(Email email) {

        String query = "INSERT INTO email_general(EMNO,SBJCT,TOADD,FRADD,TSTMP,EBODY,ATTCH,CCADD,MSGNO," +
                "FREZE) SELECT IFNULL(max(EMNO),0)+1,?,?,?,?,?,?,?,?,? from EMAIL_GENERAL";

        // Connection con = getConnection();
        PreparedStatement statement = null;

        System.out.println(email);

        try {
            statement = static_con.prepareStatement(query);
            statement.setString(1, email.getSubject());
            statement.setString(2, email.getToAddressString());
            statement.setString(3, email.getFromAddressString());
            statement.setString(4, email.getTimestamp());
            statement.setString(5, email.getBody());
            statement.setString(6, email.getAttch());
            statement.setString(7, email.getCcAddressString());
            statement.setInt(8, email.getMsgNo());
            statement.setBoolean(9, email.isFreze());
            statement.executeUpdate();

            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public List<Email> readAllEmailsGeneral(String where) {

        String query = "SELECT EMNO,MSGNO,SBJCT,FRADD,TOADD,CCADD,TSTMP,EBODY,ATTCH FROM EMAIL_GENERAL";

        if (where == null) {
            query = query + " ORDER BY EMNO DESC";
        } else {
            query = query + where + " ORDER BY EMNO DESC";
        }

        List<Email> allEmails = new ArrayList<>();

        try {
            // Connection con = getConnection();
            PreparedStatement statement = static_con.prepareStatement(query);
            System.out.println(query);
            ResultSet set = statement.executeQuery();
            //-------------Creating Email-------------
            if (!set.isBeforeFirst()) {
                return null;
            }

            while (set.next()) {
                Email email = new Email();
                email.setEmailNo(set.getInt("EMNO"));
                email.setMsgNo(set.getInt("MSGNO"));
                email.setSubject(set.getString("SBJCT"));
                email.setTimestamp(set.getString("TSTMP"));

                // Note, MM is months, not mm
                DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                DateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm a");

                Date date = null;
                try {
                    date = inputFormat.parse(email.getTimestamp());
                    String outputText = outputFormat.format(date);
                    email.setTimeFormatted(outputText);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    System.out.println(e);
                    email.setTimeFormatted("");
                }

                email.setBody(set.getString("EBODY"));
                email.setAttch(set.getString("ATTCH"));
                // particular email

                //------From Address
                String[] from = set.getString("FRADD").split("\\^");
                Address[] fromAddress = new Address[from.length];
                for (int i = 1, j = 0; i < from.length; i++, j++) {
                    try {
                        if (from[j] != null)
                            fromAddress[j] = new InternetAddress(from[i]);
                    } catch (AddressException e) {
                        System.out.println("Missing Character");
                        fromAddress[j] = new InternetAddress();
                        continue;
                    }
                }
                email.setFromAddress(fromAddress);

                //-----To Address
                String[] to = set.getString("TOADD").split("\\^");
                Address[] toAddress = new Address[to.length];
                for (int i = 1, j = 0; i < to.length; i++, j++) {
                    try {
                        toAddress[j] = new InternetAddress(to[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setToAddress(toAddress);

                //----- CC Address
                String[] cc = set.getString("CCADD").split("\\^");
                Address[] ccAddress = new Address[cc.length];
                for (int i = 1, j = 0; i < cc.length; i++, j++) {
                    try {
                        ccAddress[j] = new InternetAddress(cc[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setCcAddress(ccAddress);

                allEmails.add(email);
            }

            // doRelease(con);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return allEmails;
    }

    public void insertEmailSent(Email email) {
        String query = "INSERT INTO EMAIL_SENT(EMNO,SBJCT,FRADD,TOADD,CCADD,BCCADD,TSTMP,EBODY,ATTCH,UCODE,FREZE,ESNO" +
                ") SELECT IFNULL(max(EMNO),0)+1,?,?,?,?,?,?,?,?,?,?,? from EMAIL_SENT";

        PreparedStatement statement = null;

        System.out.println(email.getTimestamp());

        try {
            statement = static_con.prepareStatement(query);
            statement.setString(1, email.getSubject());
            statement.setString(2, email.getFromAddressString());
            statement.setString(3, email.getToAddressString());
            statement.setString(4, email.getCcAddressString());
            statement.setString(5, email.getBccAddressString());
            statement.setString(6, email.getTimestamp());
            statement.setString(7, email.getBody());
            statement.setString(8, email.getAttch());
            statement.setInt(9, user.getUCODE());
            statement.setBoolean(10, false);
            statement.setInt(11, email.getEmailStoreNo());
            statement.executeUpdate();

            statement.close();

            String[] allEmails = (email.getToAddressString() + "^"
                    + email.getCcAddressString() + "^"
                    + email.getBccAddressString()).split("\\^");

            emailPhoneQueries.emailsListInsertion(allEmails);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Email> readAllEmailsSent(String where) {

        String query = "SELECT EMNO,SBJCT,FRADD,TOADD,CCADD,BCCADD,TSTMP,EBODY,ATTCH,U.FNAME FROM EMAIL_SENT E, users" +
                " U WHERE E.UCODE = U.UCODE";

        if (where == null) {
            query = query + " ORDER BY EMNO DESC";
        } else {
            query = query + where;
        }

        List<Email> allEmails = new ArrayList<>();

        try {
            // Connection con = getConnection();
            PreparedStatement statement = static_con.prepareStatement(query);
            System.out.println(query);
            ResultSet set = statement.executeQuery();
            //-------------Creating Email-------------
            if (!set.isBeforeFirst()) {
                return null;
            }

            while (set.next()) {
                Email email = new Email();
                email.setEmailNo(set.getInt("EMNO"));
                email.setSubject(set.getString("SBJCT"));
                email.setTimestamp(set.getString("TSTMP"));

                // Note, MM is months, not mm
                DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                DateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm a");

                Date date = null;
                try {
                    date = inputFormat.parse(email.getTimestamp());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String outputText = outputFormat.format(date);
                email.setTimeFormatted(outputText);

                email.setBody(set.getString("EBODY"));
                email.setAttch(set.getString("ATTCH"));

                //------From Address
                String[] from = set.getString("FRADD").split("\\^");
                Address[] fromAddress = new Address[from.length];
                for (int i = 1, j = 0; i < from.length; i++, j++) {
                    try {
                        fromAddress[j] = new InternetAddress(from[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setFromAddress(fromAddress);

                //-----To Address
                String[] to = set.getString("TOADD").split("\\^");
                Address[] toAddress = new Address[to.length];
                for (int i = 1, j = 0; i < to.length; i++, j++) {
                    try {
                        toAddress[j] = new InternetAddress(to[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setToAddress(toAddress);

                //----- CC Address
                String[] cc = set.getString("CCADD").split("\\^");
                Address[] ccAddress = new Address[cc.length];
                for (int i = 1, j = 0; i < cc.length; i++, j++) {
                    try {
                        ccAddress[j] = new InternetAddress(cc[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setCcAddress(ccAddress);

                //----- CC Address
                String[] bcc = set.getString("BCCADD").split("\\^");
                Address[] bccAddress = new Address[bcc.length];
                for (int i = 1, j = 0; i < bcc.length; i++, j++) {
                    try {
                        bccAddress[j] = new InternetAddress(bcc[i]);
                    } catch (AddressException e) {
                        e.printStackTrace();
                    }
                }
                email.setBccAddress(bccAddress);
                email.setUser(set.getString("FNAME"));

                allEmails.add(email);
            }

            // doRelease(con);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return allEmails;
    }


    public void lockEmail(Email email, int op) {        //0 Unlock 1 Lock

        String query = " UPDATE EMAIL_STORE " +
                " SET LOCKD = ? " +
                " WHERE EMNO = ? ";

        // Connection con = getConnection();
        PreparedStatement statement = null;

        try {
            statement = static_con.prepareStatement(query);

            if (op == 1) {  //Locking
                statement.setInt(1, user.getUCODE());
                statement.setInt(2, email.getEmailNo());
            } else if (op == 0) {   //Unlocking
                statement.setInt(1, 0);
                statement.setInt(2, email.getEmailNo());
            }
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // doRelease(con);
        }

    }

    public void solvEmail(Email email, String flag, Users user, boolean choice, String msg) {

        String query = " UPDATE EMAIL_STORE " +     //Query to Update Email status to solve
                " SET ESOLV = ?, " +
                " SOLVBY = ?, " +
                " SOLVTIME = ? " +
                " WHERE EMNO = ? ";

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(Calendar.getInstance().getTime());
        email.setTimestamp(timeStamp);

        PreparedStatement statement = null;

        try {
            statement = static_con.prepareStatement(query);
            statement.setString(1, flag);
            statement.setInt(2, user.getUCODE());
            statement.setString(3, email.getTimestamp());
            statement.setInt(4, email.getEmailNo());
            statement.executeUpdate();
            statement.close();

            if (eSetting.isSolv() && choice) {
                solvResponder(email, msg);
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // doRelease(con);
        }

    }

    private void solvResponder(Email email, String msg) {

        String sb = "Ticket Number: " + email.getEmailNo() + " Resolved";

        String bd = msg;

        Email send = new Email();
        send.setSubject(sb);
        send.setToAddress(email.getFromAddress());
        send.setCcAddress(email.getCcAddress());
        send.setBody(bd);

        emailControl.sendEmail(send, null);

    }

    public void ArchiveEmail(int type, String where) {    //Verb

        String query = "";

        switch (type) {
            case 1:
                query = "UPDATE EMAIL_STORE SET FREZE = 1 WHERE ";
                break;
            case 2:
                query = "UPDATE EMAIL_GENERAL SET FREZE = 1 WHERE ";
        }

        query = query + where;

        // Connection con = getConnection();
        PreparedStatement statement = null;

        try {
            statement = static_con.prepareStatement(query);
            statement.executeUpdate();

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void EmailsListInsertion(String[] emails) {

        String emailList = "INSERT INTO EMAIL_LIST(EM_ID,EM_NAME,CL_ID,UCODE,CS_ID) " +
                "SELECT IFNULL(max(EM_ID),0)+1,?,?,?,? from EMAIL_LIST";

        try {
            PreparedStatement statement = null;

            for (int i = 0; i < emails.length; i++) {   //Inserting Emailss
                statement = null;
                if (emails[i] == null || emails[i].equals(""))
                    continue;

                statement = static_con.prepareStatement(emailList);
                statement.setString(1, emails[i]);
                statement.setInt(2, 0);
                statement.setInt(3, 0);
                statement.setInt(4, 0);
                statement.executeUpdate();
            }

            if (statement != null)
                statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
