// This file is part of Arezzo.

// Arezzo is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Arezzo is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

// Copyright (C) Pierre Jouvelot, 1997-2014, MINES ParisTech

// Contributors : Jerome Segard (2000)

package cnpmusic.arezzo ;

import cnpmusic.arezzo.User ;
import cnpmusic.arezzo.Options ;
import cnpmusic.arezzo.Sequence ;
import cnpmusic.arezzo.Chiffrage ;
import cnpmusic.arezzo.Chord ;
import cnpmusic.arezzo.Tonality ;
import cnpmusic.arezzo.Mesure ;


// Adapted from the book _Java in a Nutshell_ by David Flanagan.
// Written by David Flanagan. Copyright (c) 1996 O'Reilly & Associates.

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Properties;
import java.util.Date;
import java.lang.*;
import java.text.* ;

import javax.mail.*;
import javax.activation.*;
import javax.mail.internet.*;

public class Server 
  extends Thread 
  implements Constants 
{
  Server arezzo_server ;
  SharingServer sharing_server ;
  CommandServer command_server ;

  ServerSocket arezzo_server_socket ;

  static String file_separator ;

  static String data_directory ;
  static String bin_directory;
  static String applet_directory;

  static Glossary en_glossary, fr_glossary ;
 
  public static void main( String[] args ) 
    throws Exception 
  {
    if (args.length != 1) {
      fail( null, "Syntax: java Server <full_machine_name>" ) ;
    }
    String server_name = args[ 0 ] ;

    Properties ps = System.getProperties() ;
    file_separator = (String)ps.get( "file.separator" ) ;

    data_directory = 
      (String)ps.get( "user.dir" )+"/"+Constants.data_directory;
    bin_directory = 
      (String)ps.get( "user.dir")+"/"+ Constants.bin_directory;
    applet_directory = 
      (String)ps.get( "user.dir")+"/"+ Constants.applet_directory;

    SecurityManager sm = System.getSecurityManager() ;

    if( sm == null ) {
      System.err.println( "Warning: no security manager -- "+
			  "check write permission in "+data_directory ) ;
    }
    else {
      sm.checkWrite( data_directory ) ;
      sm.checkRead( data_directory ) ;
      sm.checkRead( bin_directory ) ;
    }
    new Server( server_name, port );
  }

  Server( String server_name, int port ) {
    arezzo_server = this ;
    //    sharing_server = new SharingServer( server_name ) ;
    command_server = new CommandServer( command_port, arezzo_server ) ;

    try { 
      Server.log( "Server", "Trying to listen on port " + port );
      arezzo_server_socket = new ServerSocket( port ); 
    }
    catch( IOException e ) { 
      fail( e, "Exception creating Arezzo server socket" ); 
    }
    Server.log( "Server", "Listening on port " + port );
    try {
      en_glossary = new Glossary( "Glossary_en" ) ;
      Server.log( "Server", "English glossary loaded" ) ;
      fr_glossary = new Glossary( "Glossary_fr" ) ;
      Server.log( "Server", "French glossary loaded" ) ;
    }
    catch( IOException e ) {
      System.err.println( "Cannot access Arezzo glossary files" ) ;
      return ;
    }
    start();
  }
    
  static String slashize( String s ) {
    return s.replace( '/', file_separator.charAt( 0 )) ;
  }

  public void run() {
    Vector connections = new Vector() ;

    try {
      while( true ) {
	Socket client_socket = arezzo_server_socket.accept();
	InetAddress ia = client_socket.getInetAddress() ;
	Server.log( "Server", 
		    "Creating connection to "+ia.getHostName()+
		    " ("+ia.getHostAddress()+")" ) ;
	connections.addElement( new Connection( client_socket )) ;
      }
    }
    catch( IOException e ) { 
      Server.log( "Server", 
		  "Exception while listening for connections:"+e );
    }
    Enumeration elts = connections.elements() ;

    while( elts.hasMoreElements()) {
      try {
	Connection c = (Connection)elts.nextElement() ;

	if( c.isAlive()) {
	  Server.log( "Server", 
		      "Stopping connection for "+
		      ((c.user_name == null) ? "-" : c.user_name) ) ;
	  c.join() ;
	}
      }
      catch( InterruptedException e ) {
	fail( e, "while joining connection" ) ;
      }
    }
  }

  public static void fail(Exception e, String msg) {
    System.err.println(msg + ": " +  e);
    e.printStackTrace() ;
  }

  static public synchronized void log( String u, String s ) {
    String st = 
      DateFormat.getDateTimeInstance().format( new Date()) ;    

    System.out.println( u+"/"+st+"/ "+s ) ;
    System.out.flush() ;
  }
}

class CommandServer
  extends Thread
{
  Server server ;

  ServerSocket command_server_socket ;

  CommandServer( int port, Server s ) {
    try {
      command_server_socket = new ServerSocket( port ); 
    }
    catch( IOException e ) { 
      Server.fail( e, "Exception creating command server socket" ); 
    }
    server = s ;
    Server.log( "CommandServer", "Listening on port " + port );
    start() ;
  }

  public void run() {
    try {
      while( true ) {
	Socket command_socket = command_server_socket.accept();
	DataInputStream in = 
	  new DataInputStream( command_socket.getInputStream());
	BufferedReader sr = 
	  new BufferedReader( new InputStreamReader( in )) ;
	processCommand( sr.readLine()) ;
      }
    }
    catch( IOException e ) { 
      Server.log( "CommandServer", 
		  "IOException while listening for commands:"+e );
    }
    catch( Exception e ) { 
      Server.log( "CommandServer", 
		  "Exception while listening/processing commands:"+e );
    }
    Server.log( "CommandServer", "Command server stopped" ) ;
  }

  private void processCommand( String cmd ) 
    throws Exception
  {
    if( cmd.equals( "stop" )) {
      Server.log( "CommandServer", "Stopping command server ..." ); 
      command_server_socket.close() ;

      Server.log( "CommandServer", "Stopping Arezzo server ..." ); 
      server.arezzo_server_socket.close() ;
      server.join();

      Server.log( "CommandServer", "Stopping sharing server ..." ) ;
      server.sharing_server.stop() ;

      Server.log( "CommandServer", "Servers stopped : exiting" ) ;
      System.exit( 0 ) ;     // Needed since jsdt keeps impl threads.
    }
    if( cmd.equals( "ping" )) {
      Server.log( "CommandServer", "Pong ..." ); 
    }
  }
}

class Connection 
  extends Thread 
{
  protected Socket client;
  protected DataInputStream in;
  protected OutputStream out;
  protected ObjectOutputStream oos ;
  protected ObjectInputStream ois ;

  String user_name ;

  // Initialize the streams and start the thread

  public Connection( Socket client_socket ) {
    client = client_socket;

    try { 
      in = new DataInputStream( client.getInputStream());
      out = client.getOutputStream();
    }
    catch (IOException e) {
      try { client.close(); } catch (IOException e2) { ; }
      System.err.println("Exception while getting socket streams: "+e);
      return;
    }
    this.start();
  }
    
  private void clearTmp( String dir_name, String[] tmp_files ) 
    throws OptionalDataException, IOException, ClassNotFoundException 
  {
    for( int i = 0 ; i<tmp_files.length ; i++ ) {
      deleteFile( Server.slashize( dir_name+"/"+tmp_files[i] )) ;
    }
  }

  // List scores.
  // Clean temporary files (MIDI,ABC and tmp). Mail files are removed in 
  // getSequence.
  //
  private void listScores() 
    throws OptionalDataException, IOException, ClassNotFoundException 
  {
    Server.log( user_name,  "Listing Scores: "+user_name ) ;

    String dir_name = Server.slashize( Server.data_directory+"/"+user_name ) ;
    File dir = new File( dir_name ) ;

    if( !dir.exists()) {
      Server.log( user_name, "Creating user score directory:"+user_name ) ;
      if( !dir.mkdir()) {
	System.err.println( "Unable creating user directory :"+
			    dir.getName()) ;
      }
    }
    clearTmp( dir_name, dir.list( new FilenameFilter() {
	public boolean accept( File dir, String name ) {
	  return 
	    name.endsWith( Constants.midi_suffix ) ||
	    name.endsWith( Constants.abc_suffix ) ||
	    name.endsWith( Constants.ps_suffix ) ;  
	}
      })) ;
    String [] notes_files = dir.list( new FilenameFilter() {
	public boolean accept( File dir, String name ) {
	  return 
	    name.endsWith( Constants.notes_suffix ) &&
	    !name.endsWith( Constants.mail_suffix ) ;
	}
      }) ;
    for( int i = 0 ; i<notes_files.length ; i++ ) {
      oos.writeObject( notes_files[i] ) ;
    }
    oos.writeObject( ".List done" ) ;
    oos.flush() ;
    Server.log( user_name,  "Listing completed" ) ;
  }

  private void getSequence() 
    throws Exception 
  {
    String filename = (String) ois.readObject() ;
    filename = Server.slashize( filename ) ;
    Server.log( user_name,  "Getting sequence: "+filename ) ;
    FileInputStream fis = new FileInputStream( filename ) ;
    ObjectInputStream ois = new ObjectInputStream( fis ) ;
	
    oos.writeObject( ois.readObject()) ;

    // Following try/catch are for backward compatibility

    Object prefs ;
    try {
      prefs = ois.readObject() ;
    }
    catch( EOFException e ) {
      prefs = "No prefs" ;
    }
    oos.writeObject( prefs ) ;

    Object comments ;
    try {
      comments = ois.readObject() ;
    }
    catch( EOFException e ) {
      comments = "" ;
    }
    oos.writeObject( comments ) ;
    oos.flush() ;
    fis.close() ;
    ois.close() ;

    if( filename.endsWith( Constants.mail_suffix )) {
      deleteFile( filename ) ;
    }
    Server.log( user_name,  "Sequence gotten" ) ;
  }
  
  private void deleteFile( String filename ) 
    throws OptionalDataException, IOException, ClassNotFoundException 
  {
    Server.log( user_name,  "Deleting: "+filename ) ;
    File f = new File( filename ) ;
    
    if( !f.exists()) {
      System.err.println( "Unexisting file: "+filename ) ;
    }
    if( !f.delete()) {
      System.err.println( "Unable to delete file: "+filename ) ;
    }
  }
  
  private void explainWord() 
    throws OptionalDataException, IOException, ClassNotFoundException 
  {
      String arg = (String) ois.readObject() ;
      Locale loc = (Locale) ois.readObject() ;
      Server.log( user_name,  "Explaining |"+arg+"|" ) ;

      Glossary g = (loc.getLanguage().equals( "fr" )) ? 
	Server.fr_glossary : 
	Server.en_glossary ;

      String exp = g.find( arg.toLowerCase() );
      oos.writeObject( g.highlightWords( exp )) ;
      oos.flush() ;
      Server.log( user_name,  "Explain done" ) ;
  }

  private void listCourses() 
    throws Exception 
  {
    String thema = (String) ois.readObject() ;
    Locale loc = (Locale) ois.readObject() ;
    String course = thema+"_"+loc.getLanguage()+".pedago" ;

    try {
	BufferedReader br = new BufferedReader( new FileReader( course )) ;

	for( String s = br.readLine() ; s != null ; s = br.readLine()) {
	    oos.writeObject( s ) ;
	}
    }
    catch( Exception f ) {
	System.err.println( "No course "+course+"("+f+")" );       
    }
    oos.writeObject( ".List done" ) ;
    oos.flush() ;
    Server.log( user_name, "Course list done:"+course ) ;
  }

  private void saveMidi() 
    throws OptionalDataException, IOException, ClassNotFoundException 
  {
      String filename = (String)ois.readObject() ;
      Server.log( user_name,  "Opening MIDI file "+filename ) ;

      FileOutputStream file = new FileOutputStream( filename ) ;
      Server.log( user_name,  "File "+filename+" opened" ) ;
	
      byte[] data = new byte[1024] ;
      int nbytes ;
	
      while( (nbytes=in.read( data, 0, data.length )) != -1 ) {
	file.write( data, 0, nbytes ) ;
      }
      Server.log( user_name,  "Closing "+filename ) ;
      file.close() ;
  }

  private void saveSequence() 
    throws OptionalDataException, IOException, ClassNotFoundException 
  {
      String filename = (String)ois.readObject() ;
      Server.log( user_name,  "Opening file "+filename ) ;

      ObjectOutputStream foos = 
	new ObjectOutputStream ( new FileOutputStream( filename )) ;
      Server.log( user_name,  "File "+filename+" opened" ) ;
      foos.writeObject( ois.readObject()) ;
      foos.writeObject( ois.readObject()) ;
      Object s = ois.readObject() ;
      foos.writeObject( s ) ;
      Server.log( user_name,  "Closing "+filename ) ;
      foos.close() ;
	
      oos.writeObject( "Done" ) ;
  }

  private void saveABC( String filename, String abc ) 
    throws Exception 
  {
    FileWriter fw = new FileWriter( filename );
    Server.log( user_name, " ABC file "+filename+" opened");
    fw.write( abc );
    Server.log( user_name, " Closing "+filename);
    fw.close();
  }
    
  private void mailUser( String command ) 
    throws Exception 
  {
      String filename = (String) ois.readObject();
      String subject = (String) ois.readObject();
      String abc = (String) ois.readObject() ;
      User u = (User) ois.readObject() ;

      String to = u.getEmail() ; 
      Server.log( user_name,  
		  "Mailing: "+filename+" to user ("+command+"): "+to );
      String file_to_mail = "" ;
      String absolute_filename = 
	Server.slashize( Server.data_directory+"/"+u.getName()+"/"+filename );

      if( command.equals( ".Mail MIDI" )) {
	file_to_mail = absolute_filename+Constants.midi_suffix ;
      }
      else {
	String abc_filename = absolute_filename+Constants.abc_suffix ;

	saveABC( abc_filename, abc );

	if( command.equals( ".Mail PS" )) {
	  Server.log( user_name, " Converting "+filename+" to PS ...");

	  int rand = new Random().nextInt();
	  file_to_mail = 
	    Server.slashize( Server.data_directory+"/"+u.getName()+"/"+
			     "mailMe_"+rand+Constants.ps_suffix ) ;
	  String abcm2ps = 
	    Server.slashize( Server.bin_directory+"/abcm2ps" );    
	  Runtime.getRuntime().exec( new String[] {abcm2ps, 
						   "-O", file_to_mail, 
						   abc_filename} );
    
	  Server.log( user_name, " Conversion completed in "+file_to_mail );
	}
	else if( command.equals( ".Mail ABC" )) {
	  file_to_mail = abc_filename ;
	}
      }
      oos.writeObject(sendMail(subject, to, to, file_to_mail, false));
      Server.log( user_name, "Mailing user done");
  }

  private void informServer() 
    throws Exception 
  {
    String i = (String) ois.readObject() ;

    Server.log( user_name,  i ) ;
    oos.writeObject( "Done" ) ;
  }
    
  private void sendError( Exception e ) 
  {
    String s = "Server error: "+e ;

    System.err.println( s ); 
    e.printStackTrace() ;
    
    try {
      oos.writeObject( new Exception( s )) ;
    }
    catch( Exception f ) {
      System.err.println( "Lost connection" );       
    }
  }

  // Verifications sur le serveur

  static ResourceBundle messages = null ;

  private static synchronized void setLanguage() 
    throws Exception
  {
    if( messages == null ) {
      String filename = 
	Server.slashize( Server.applet_directory+"/Bundle/"+
			 "MessagesBundle_fr_FR.properties" ) ;
      FileInputStream fis = new FileInputStream( filename ) ;
      messages = new PropertyResourceBundle( fis ) ;
      new Harmony( messages ) ;
    }
  }

  private Options getOptions( StringTokenizer st )
    throws Exception
  {
    int nb = Integer.parseInt( st.nextToken()) ;
    Options opts = new Options() ;

    for( int i = 0 ; i<nb ; i++ ) {
      String o = st.nextToken() ;
      boolean b = (st.nextToken().equals( "1" )) ;
      
      Server.log( "Server", "Setting option "+o+" to "+b ) ;
      opts.put( o, new Boolean( b )) ;
    }
    return opts ;
  }

  private Sequence getSequence( StringTokenizer st, Options opts ) 
    throws Exception
  {
    Sequence s = new Sequence( st ) ;
    s.check_options = opts ;
    Harmony.me.score = new Score( "cnpmusic", s ) ;
    return s ;
  }

  static final int harmony_check = 0 ;
  static final int melody_check = 1 ;
  static final int counterpoint_check = 2 ;

  private String runCheck( StringTokenizer st, Options opts, String result )
    throws Exception
  {
    try {
      int check_type = Integer.parseInt( st.nextToken()) ;

      switch( check_type ) {
      case harmony_check:
	Sequence sh = getSequence( st, opts ) ;
	Server.log( "Server", 
		    "Performing check on sequence\n"+sh.sprintf()) ;
	sh.harmonyCheck() ;
	break ;
      case melody_check:
	int voice = Integer.parseInt( st.nextToken()) ;
	Sequence sm = getSequence( st, opts ) ;
	Server.log( "Server", "Performing check on voice "+voice ) ;
	sm.melodyCheck( voice ) ;
	break ;
      case counterpoint_check:
	int[] voices = new int[Integer.parseInt( st.nextToken())] ;

	for( int i = 0 ; i<voices.length ; i++ ) {
	  voices[ i ] = Integer.parseInt( st.nextToken()) ;
	}
	Sequence sc = getSequence( st, opts ) ;
	Server.log( "Server", "Performing check on voices "+voices ) ;
	sc.counterpointCheck( voices ) ;
	break ;
      default:
	result = "CnpMusic error : Unknown check type "+check_type ;	
      }
    }
    catch( CheckException e ) {
      result = e.getMessage() ;
    }
    catch( Exception e ) {
      result = ".Check: "+e.getMessage() ;
      Server.fail( e, result ) ;
    }
    return result ;
  }

  private String runAdvise( StringTokenizer st, Options opts, String result )
    throws Exception
  {
    try {
      int check_type = Integer.parseInt( st.nextToken()) ;
      int chord_number = Integer.parseInt( st.nextToken()) ;
      
      switch( check_type ) {
      case harmony_check:
	Sequence sh = getSequence( st, opts ) ;
	Chord chord = (Chord)sh.elementAt( chord_number ) ;
	Server.log( "Server", 
		    "Advising chord "+chord_number+"\n"+chord.sprintf()) ;
	sh.harmonyAdvise( chord ) ;
	break ;
      case melody_check:
      case counterpoint_check:
	break ;
      default:
      	result = "CnpMusic error : Unknown check type "+check_type ;	
      }
    }
    catch( AdviseException e ) {
      result = e.getMessage() ;
    }
    catch( Exception e ) {
      result = ".Advise: "+e.getMessage() ;
      Server.fail( e, result ) ;
    }
    return result ;
  }

  private String runJazz( StringTokenizer st )
  {
    String result = "" ;

    try {
      Sequence s = getSequence( st, Sequence.default_check_options ) ;

      for( int i = 0 ; i<s.size() ; i++ ) {
	Chord c = (Chord)s.elementAt( i ) ;
	result += 
	  c.degre.jazz( c, s.tonality )+","+
	  c.chiffrage.jazz( c, s.tonality )+
	  ":" ;
      }
    }
    catch( Exception e ) {
      result = ".Jazz: "+e.getMessage() ;
      Server.fail( e, result ) ;
    }
    return "jazz:"+result ;
  }

  private String runChiffrage( StringTokenizer st ) {
    String result = "" ;

    try {
      st.nextToken() ; // drop version
      Tonality t = new Tonality( st ) ;
      new Mesure( st ) ;
      String s = st.nextToken() ;

      Chiffrage.Jazz j = new Chiffrage.Jazz( s ) ;
      j.setChiffrage( t ) ;
      result = j.sprintf() ;
    }
    catch( Exception e ) {
      result = ".Chiffrage: "+e.getMessage() ;
      Server.fail( e, result ) ;
    }
    return "chiffrage:"+result ;
  }


  // Syntaxe d'echange :
  // 
  // [Constants.arezzo_mode|Constants.cnpmusic_mode] ;; See run() 
  // [".Check"|".Advise"] 
  //   nb_options (option [0|1])*
  //   case {
  //     harmony_check
  //     melody_check voice
  //     counterpoint_check nb_voices voice*
  //   }
  //   Sequence                ;; =version+tonalite+mesure+chord*) 
  // [".Jazz"]
  //   Sequence                ;; =version+tonalite+mesure+chord*) 
  // [".Chiffrage"]
  //   version+tonalite+mesure
  //   chiffrage_jazz
  // Constants.cnpmusic_mode

  private void runCnpmusic() 
    throws Exception
  {
    String input = "" ;

    for( int c ; (c=in.read()) != Constants.cnpmusic_mode ; ) {
      input += (char)c ;
    }
    StringTokenizer st = new StringTokenizer( input ) ;
    String command = st.nextToken() ;
    String result ;
    
    Server.log( "Server", "Processing command :"+command ) ;
    setLanguage() ;

    if( command.equals( ".Jazz" )) {
      result = runJazz( st ) ;
    }
    else if( command.equals( ".Chiffrage" )) {
      result = runChiffrage( st ) ;
    }
    else {
      Options opts = getOptions( st ) ;

      result = 
	(command.equals( ".Check" )) ?
	runCheck( st, opts, messages.getString( "Harmony.NoErrors" )) :
	(command.equals( ".Advise" )) ?
	runAdvise( st, opts, messages.getString( "Harmony.NoAdvice" )) :
	"CnpMusic error : Unknown command "+command ;
    }
    new DataOutputStream( out ).writeBytes( result ) ;
    Server.log( "Server", "Command processed\n"+result ) ;
  }

  private void runArezzo() 
    throws Exception 
  {
    String command = (String) ois.readObject() ;
    user_name = (String) ois.readObject() ;

    if( command.equals( ".List Scores" )) {
      listScores();
    } 
    else if( command.equals( ".List Courses" )) {
      listCourses() ;
    }
    else if( command.equals( ".Get Sequence" )) {
      getSequence();
    }
    else if( command.equals( ".Delete File" )) {
      String filename = (String) ois.readObject() ;
      deleteFile( filename );
      oos.writeObject( "Done" ) ;
    }
    else if( command.equals( ".Explain Word" )) {
      explainWord();
    }
    else if( command.equals( ".Save MIDI" )) {
      saveMidi();
    }
    else if( command.equals( ".Save Notes" )) {
      saveSequence();
    }
    else if( command.equals(".Mail PS") || 
	     command.equals(".Mail ABC") ||
	     command.equals(".Mail MIDI")) {
      mailUser( command ) ;
    }
    else if( command.equals( ".Inform Server" )){
      informServer() ;
    }
    else {
      sendError( new Exception( "Unknown command:"+command )) ;
    }
  }

// Provide the service.

  public void run() 
  {
    try {
      byte mode = in.readByte() ;
      Server.log( "Server", "Getting new request from "+mode ) ;

      switch( mode ) {
      case -1 :
	throw new EOFException() ;
      case Constants.arezzo_mode :
	ois = new ObjectInputStream( client.getInputStream()) ;
	oos = new ObjectOutputStream( client.getOutputStream()) ;
	runArezzo() ;
	break ;
      case Constants.cnpmusic_mode :
	runCnpmusic() ;
	break ;
      default :
	Server.log( "Server", "Unexpected mode "+mode ) ;
	throw new IOException() ;
      }
    }
    catch( Exception e ) { 
      sendError( e ) ;
    }
    finally { 
      try {
	in.close() ;
	out.close() ;
	ois.close() ;
	oos.close() ;
	client.close();
      }
      catch (Exception e2) {
	// System.err.println( "Cannot close socket "+e2 ) ;
      }
    }
  }

// Mail functions

  public String 
  sendMail( String subject, String to, String from, String content, 
	    boolean in_html) {

    String mailhost = Constants.smtp_host ;
    String mailer = "sendMail";

    Properties props = System.getProperties();
    props.put("mail.smtp.host", mailhost);
    
    Session session = Session.getDefaultInstance(props, null);
    
    try {
      Message msg = new MimeMessage(session);
      msg.setRecipients(Message.RecipientType.TO,
			InternetAddress.parse( to, false ));
      msg.setFrom(new InternetAddress( from ));
      msg.setSubject( subject );                
      msg.setHeader("X-Mailer", mailer);
      msg.setSentDate(new Date());

      if(in_html) {
	msg.setDataHandler(new DataHandler( content, "text/html"));
      }
      else {
	  FileDataSource fds = new FileDataSource(content);
	msg.setDataHandler(new DataHandler(fds));
	msg.setFileName(fds.getName());
      }
	  
      Transport.send(msg);
      return "Sending done";
    }
    catch (MessagingException me) {
      return me.getMessage();
    }
    catch (Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }
  
}

class Glossary 
  extends Hashtable
{
  Glossary( String filename ) 
    throws IOException 
  {
    super() ;
    BufferedReader input = 
      new BufferedReader( new FileReader( filename ));
    
    while( true ) {
      String key, value;

      if( (key=input.readLine()) == null) {
	break ;
      }    
      value = input.readLine();
      put( key, value );
    }
  }
	
  String find( String s ) {
    String def, temp;
    
    if(s.endsWith("s") && !s.endsWith("ss")) {
      temp = s.substring(0, s.length() - 1);
    }
    else {
      temp = s ;
    }
    def = (String)get( temp );
    return (def == null) ? "" : highlightWords( def );
  }
  
  String highlightWords(String text) {
    StringTokenizer line = 
      new StringTokenizer(text, Harmony.separators, true);
    String output = "";
    boolean needs_leading_blank = false ;
    
    while( line.hasMoreTokens()) {
      String token = line.nextToken();
      
      if( containsKey( token )) {
	token = token.toUpperCase() ;
      }
      output = 
	output.concat( ((needs_leading_blank) ? " " : "") + token ) ;
      needs_leading_blank = 
	(Harmony.separators_without_leading_blank.indexOf( token ) != -1) ;
    }
    return output ;
  }
}

