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

import cnpmusic.arezzo.* ;
import java.awt.* ;
import java.awt.event.* ;
import java.applet.* ;
import java.io.*;
import java.util.* ;
import java.net.* ;
import java.text.* ;

import com.sun.media.jsdt.* ;

public class Harmony 
  extends Panel
  implements Constants 
{
  Score score ;
  User user;

  static Harmony me ;

  // Enclosing applet

  Applet applet ;

  // Server interface

  Client client ;

  // User interface

  Dimension dimension ;

  Scrollbar scroll ;
  private TextArea diagnostic ;

  // Management of Java Shared Data Toolkit

  LocalListeners local_listeners ;
  SharingListeners sharing_listeners ; 
  HarmonyListeners current_listeners ;

  Vector shared_menus ;
  Vector sharing_ops_menus ;
    
  String server ;

  // Internationalization

  ResourceBundle messages ;
  Locale locale ;

  // Database

  Vector score_names ;
  String score_name ;

  // Double buffering graphics (adapted from the FAQ)
 
  private Image offScreenImage;
  private Dimension offScreenSize;
  private Graphics offScreenGraphics;
 
  Harmony( ResourceBundle rb ) {
    me = this ;
    messages = rb ;
  }

  Harmony( Applet a, Dimension d, String s, Locale l ) {
    applet = a ;
    dimension = d ;
    server = s ;
    locale = l ;
    me = this ;
    messages = ResourceBundle.getBundle( messages_file, locale ) ;
    client = new Client( server ) ;
  }

  public void login( String auto_load, String u, String a ) 
  {
    String other_user = null ;
    user = new User( u, a ) ;

    try {
	client.informServer( "Connecting "+user.getName()) ;
    }
    catch( Exception e ) {
      MessageFormat mf = 
	new MessageFormat( messages.getString( "Harmony.ServerNotRunning" )) ;
      fail( mf.format( new Object[] {server} ), e ) ;
    }
    current_listeners = local_listeners = new LocalListeners() ;

    createMenuBar() ;
    createInterface() ;

    addListeners( current_listeners ) ;

    try {
	score_names = client.getScoreNames() ;
    }
    catch( Exception e ) {
      MessageFormat mf = 
	new MessageFormat( messages.getString( "Harmony.ServerNotRunning" )) ;
      fail( mf.format( new Object[] {server} ), e ) ;
      score_names = new Vector() ;
    }
    try {
      if( client.isConnected() && 
	  auto_load != null && 
	  !auto_load.equals( "" )) {
	int separator_index = auto_load.indexOf( default_separator ) ;
	String ou = auto_load.substring( 0, separator_index ) ;
	score_name = auto_load.substring( separator_index+1 ) ;
	score = 
	  new Score( score_name,
		     client.getSequence( filePrefix( ou )+notes_suffix )) ;
	other_user = ou ;
      }
      else {
	score_name = score_name_default ;
	score = new Score( score_name, 
			   (client.isConnected()) ?
			   client.getSequence( filePrefix()+notes_suffix ) :
			   new Sequence()) ;
      }
    }
    catch( Exception e ) {
      fail( "Unexpected login exception", e ) ;
    }
    if( other_user != null ) {
      MessageFormat omf = 
	new MessageFormat( messages.getString( "Harmony.Autoload" )) ;
      inform( omf.format( new String[] {score_name, other_user} )) ;
    }
    MessageFormat mf = 
	new MessageFormat( messages.getString( "Harmony.LoggedIn" )) ;
    inform( mf.format( new String[] {user.getName()})) ;
    
    repaint();

    showComments( score.sequence ) ;
  }

  static String shared_marker = "*" ;

  private void addListeners( HarmonyListeners l ) {
    addMouseListener( l ) ;
    addMouseMotionListener( l ) ;
    addKeyListener( l ) ;
    addFocusListener( l ) ;

    for( int i=0 ; i<shared_menus.size() ; i++ ) {
      MenuItem mi = (MenuItem)shared_menus.elementAt( i ) ;
      mi.addActionListener( l ) ;
      
      if( l == sharing_listeners ) {
	mi.setLabel( shared_marker+mi.getLabel()) ;
      }
    }
    scroll.addAdjustmentListener( l ) ;
  }

  private void removeListeners( HarmonyListeners l ) {
    removeMouseListener( l ) ;
    removeMouseMotionListener( l ) ;
    removeKeyListener( l ) ;
    removeFocusListener( l ) ;

    for( int i=0 ; i<shared_menus.size() ; i++ ) {
      MenuItem mi = (MenuItem)shared_menus.elementAt( i ) ;
      mi.removeActionListener( l ) ;

      if( l == sharing_listeners ) {
	mi.setLabel( mi.getLabel().substring( shared_marker.length())) ;
      }
    }
    scroll.removeAdjustmentListener( l ) ;
  }

  class HarmonyOkDialog
    extends OkDialog
  {
    HarmonyOkDialog( String s, OkDialogContent odc ) {
      super( getFrame( Harmony.me ), 
	     Harmony.me.messages.getString( s ), 
	     odc ) ;
    }
  }

  public static void fail( String s ) {
    Harmony.me.new HarmonyOkDialog( "Harmony.HarmonyError",
				    new FailMessageContent( s )) ;
  }

  public static void fail( String s, Exception e ) {
    if( !(e instanceof ConnectException )) { 
      e.printStackTrace( System.err ) ;
    }
    fail( s+":\n\nMessage -- "+e ) ;
  }

  public void inform( String s ) {
    String i = messages.getString( "Harmony.Information" )+" - "+s ;
    diagnostic.append( i+".\n" ) ;
    
    try {
      if( client.isConnected()) {
	client.informServer( i ) ;
      }
    }
    catch( ConnectException e ) {
    }
  }

  public void paint( Graphics g ) {
    if( score != null ) {
      score.paint( g ) ;
      validate() ;
      requestFocus() ;
    }
  }

  public final synchronized void update( Graphics g ) { 
    Dimension d = getSize();

    if( d.width < 0 || d.height < 0 ) {
      return ;
    }
    if( (offScreenImage == null) || 
	(d.width != offScreenSize.width) ||  
	(d.height != offScreenSize.height)) {
      offScreenImage = createImage( d.width, d.height );
      offScreenSize = d;
      offScreenGraphics = offScreenImage.getGraphics();
    }
    offScreenGraphics.setColor( getBackground()) ;
    offScreenGraphics.fillRect( 0, 0, d.width, d.height ) ;
    paint( offScreenGraphics );
    g.drawImage( offScreenImage, 0, 0, null );
    validate() ;
  }

  // Interface

  class IndexItemListener 
    implements ActionListener 
  {
    protected int index ;

    IndexItemListener(int i) {
      index = i ;
    }

    public void actionPerformed( ActionEvent e ) {}
  }

  private void enableMenus( Vector ms, boolean state ) {
    ((MenuItem)ms.elementAt( 0 )).setEnabled( !state ) ;

    for( int i = 1 ; i < ms.size() ; i++ ) {
      ((MenuItem)ms.elementAt( i )).setEnabled( state ) ;
    }
  }

  class OptionListener
    implements ActionListener
  {
    Options opts ;
    String question ;
    String name ;

    OptionListener( Options o, String q, String n ) {
      setOptions( o ) ;
      question = q ;
      name = n ;
    }
    
    public void actionPerformed( ActionEvent e ) {
      Vector v = new Vector() ;
      String opt ;

      for( int i = 0 ; i<opts.size() ; i++ ) {
	try {
	  opt = messages.getString( opts.names[i] ) ;
	}
	catch( MissingResourceException ex ) {
	  Harmony.fail( "OptionListener/actionPerformed: missing "+
			opts.names[i] ) ;
	  opt = "" ;
	}
	v.addElement( opt ) ;
      }
      String m = messages.getString( question ) ;
      ListContent lc = new ListContent( v, m ) {
	  public void callBack() {
	    updateOptions( getSelectedIndexes()) ;
	  }
	} ;
      lc.setMultipleMode( true ) ;
	  
      for( int i = 0 ; i<opts.size() ; i++ ) {
	lc.select( i ) ;
      }
      for( int i = 0 ; i<opts.size() ; i++ ) {
	if( opts.isFalse( opts.names[i] )) {
	  lc.deselect( i ) ;
	}
      }
      new HarmonyOkDialog( name, lc ) ;
    }

    void setOptions( Options o ) {
      opts = o ;
    }

    void updateOptions( int[] selected ) {
      for( int i = 0 ; i<opts.size() ; i++ ) {
	opts.reset( opts.names[i] ) ;
      }
      for( int i = 0 ; i<selected.length ; i++ ) {
	opts.set( opts.names[selected[i]] ) ;
      }
    }
  }

  private Menu newFileMenu() {
    Menu file_menu = new Menu( messages.getString( "Harmony.File" )) ;

    MenuItem new_op = new MenuItem( messages.getString( "Harmony.New" )) ;
    new_op.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	score = new Score( score_name_default, new Sequence()) ;
	repaint() ;
	inform( messages.getString( "Harmony.NewPartitionCreated" )) ;
      }
    }) ;
    file_menu.add( new_op ) ;
    MenuItem load_op = new MenuItem( messages.getString( "Harmony.Load" )) ;
    load_op.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	String s = 
	  Harmony.me.messages.getString( "Harmony.DoubleClickLoad" ) ;
	ListContent c = new ListContent( score_names, s ) {
	    public void callBack() {
	      caller.loadCallBack( me()) ;
	      super.callBack() ;
	    }
	  } ;
	new HarmonyOkDialog( "Harmony.Scores", c ) ;
      }
    }) ;
    file_menu.add( load_op ) ;
    MenuItem revert_op = 
      new MenuItem( messages.getString( "Harmony.Revert" )) ;
    revert_op.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	try {
	  Sequence olds = client.getSequence( filePrefix()+notes_suffix ) ;
	  score = new Score( score_name, olds ) ;
	  diagnostic.setText( "" ) ;
	  repaint() ;
	  inform( messages.getString( "Harmony.Reverted" )+" "+score_name ) ;
	  showComments( olds ) ;
	}
	catch( ConnectException ce ) {
	  fail( messages.getString( "Harmony.ServerNotRunning" ), ce ) ;
	}
      }
    }) ;
    file_menu.add( revert_op ) ;
    file_menu.add( "-" ) ;

    MenuItem delete_op = 
      new MenuItem( messages.getString( "Harmony.Delete" )) ;
    delete_op.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	String m = 
	  Harmony.me.messages.getString( "Harmony.DoubleClickDelete" ) ;
	ListContent c = new ListContent( score_names, m ) {
	  public void callBack() {
	    caller.deleteCallBack( me()) ;
	    super.callBack() ;
	  }
	} ;
	new HarmonyOkDialog( "Harmony.Scores", c ) ;
      }
    }) ;
    delete_op.setEnabled( client.isConnected()) ;    
    file_menu.add( delete_op ) ;
    file_menu.add( "-" ) ;

    MenuItem save_op = 
      new MenuItem( messages.getString( "Harmony.Save" )) ;
    save_op.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	saveSequence( score.sequence, filePrefix()+notes_suffix ) ;
      }
    }) ;
    save_op.setEnabled( client.isConnected()) ;
    file_menu.add( save_op ) ;
    MenuItem save_as_op = 
      new MenuItem( messages.getString( "Harmony.SaveAs" )) ;
    save_as_op.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	LineInputContent c = 
	  new LineInputContent( "Harmony.NewName", Harmony.me.score.name ) {
	      public void callBack() {
		caller.saveAsCallBack( me()) ;
		super.callBack() ;
	      }
	    } ;
	new HarmonyOkDialog( "Harmony.Save", c ) ;
      }
    }) ;
    save_as_op.setEnabled( client.isConnected()) ;
    file_menu.add( save_as_op ) ;
    return file_menu ;
  }

  private Options counterpoint_options = 
    new Options( Chord.Voice_Names, Options.option_false ) ;

  private Menu newQuestionMenu( final String what ) {
    Menu what_menu = new Menu( messages.getString( what )) ;

    MenuItem harmony_what = 
      new MenuItem( messages.getString( "Harmony.Harmony" )) ;
    String hc = what+" "+"Harmony" ;
    harmony_what.setActionCommand( hc ) ;
    local_listeners.put( hc, new ActionListener () {
	public void actionPerformed( ActionEvent e ) {
	  performHarmonyQuestion( what ) ;
	}
      }) ;
    what_menu.add( harmony_what ) ;

    Menu melody_what_menu = 
      new Menu( messages.getString( "Harmony.Melody" )) ;
    
    for( int i = 0 ; i<Chord.notes_per_chord ; i++ ) {
      MenuItem item = new MenuItem( Chord.VoiceName( i )) ;
      String cmd = what+" "+"Melody"+" "+i ;
      item.setActionCommand( cmd ) ;
      local_listeners.put( cmd, new IndexItemListener( i ) {
	  public void actionPerformed( ActionEvent e ) {
	    performMelodyQuestion( what, index ) ;
	  }
	}) ;
      melody_what_menu.add( item ) ;
    }
    shared_menus.addElement( melody_what_menu ) ;
    what_menu.add( melody_what_menu ) ;

    MenuItem counterpoint_what = 
      new MenuItem( messages.getString( "Harmony.Counterpoint" )) ;
    String cc = what+" "+"Counterpoint" ;
    counterpoint_what.setActionCommand( cc ) ;
    OptionListener olcc = 
      new OptionListener( counterpoint_options,
			  "Harmony.CounterpointSelect",
			  "Harmony.Counterpoint" ) {
	  public void updateOptions( int[] selected ) {
	    super.updateOptions( selected ) ;
	    performCounterpointQuestion( what, selected ) ;
	  }
	} ;
    local_listeners.put( cc, olcc ) ;
    what_menu.add( counterpoint_what ) ;

    shared_menus.addElement( what_menu ) ;
    return what_menu ;
  }

  class StudyMenu 
    extends Menu
  {
    StudyMenu( String t ) {
      super( t ) ;
    }

    private void addItem( final String name ) {
      MenuItem study_op = new MenuItem( "www.cnpmusic.com" ) ;
      study_op.setEnabled( client.isConnected()) ;
      study_op.addActionListener( new ActionListener () {
	  public void actionPerformed( ActionEvent e ) {
	    try {
	      URL u =  new URL( "http://www.cnpmusic.com/logiciels/" ) ;
	      applet.getAppletContext().showDocument( u, "Arezzo Study" ) ;
	      inform( messages.getString( "Harmony.Study" )) ;
	      repaint() ;
	    }
	    catch( Exception r ) {
	      fail( "Harmony.CannotFindStudyFile", r ) ;
	    } 
	  }
	}) ;
      add( study_op ) ;
    }
  }

  private Menu newOperationsMenu() {
    Menu operation_menu = 
      new Menu( messages.getString( "Harmony.Operations" )) ;

    Menu check_menu = newQuestionMenu( "Harmony.Check" ) ;
    operation_menu.add( check_menu ) ;
    Menu advise_menu = newQuestionMenu( "Harmony.Advise" ) ;
    operation_menu.add( advise_menu ) ;

    MenuItem evaluate_op = 
      new MenuItem( messages.getString( "Harmony.Evaluate" )) ;
    evaluate_op.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e ) {
	  String s = user.mainError() ;

	  if( s != null ) {
	      diagnostic.append( "Evaluation : "+
				 messages.getString( s )+"\n" ) ;
	      user.errors.remove( s ) ;
	  }
      }}) ;
    evaluate_op.setEnabled( true );
    operation_menu.add( evaluate_op ) ;
    operation_menu.add( "-" ) ;

    Menu study_menu = new Menu( messages.getString( "Harmony.Study" )) ;
    String[] themas = {"Oreille", "Rythme", "Theorie", "Lecture"} ;
    for( int i = 0 ; i<themas.length ; i++ ){
      StudyMenu o = 
	new StudyMenu( messages.getString( "Harmony."+themas[ i ])) ;
      o.addItem( themas[ i ].toLowerCase()) ;
      study_menu.add( o ) ;
    }
    study_menu.setEnabled( locale.getLanguage().equals( "fr" )) ;
    study_menu.addActionListener( new ActionListener() {
	    public void actionPerformed( ActionEvent e ) {
	    }});
    operation_menu.add( study_menu ) ;
    operation_menu.add( "-" ) ;

    MenuItem play_op = new MenuItem( messages.getString( "Harmony.Play" )) ;
    String ps = "Play MIDI" ;
    play_op.setActionCommand( ps ) ;
    OptionListener olp = 
      new OptionListener( Chord.midi_play_options,
			  "Harmony.PlaySelect",
			  "Harmony.Play" ) {
	  private int midi_counter = 0 ;

	  public void updateOptions( int[] selected ) {
	    super.updateOptions( selected ) ;
	    String tmp_prefix = filePrefix()+"_"+midi_counter++ ;

	    try {
	      client.saveMIDI( score.sequence, tmp_prefix+midi_suffix ) ;
	      URL sound = 
		new URL( applet.getCodeBase()+"/"+tmp_prefix+midi_suffix ) ;
	      inform( messages.getString( "Harmony.Playing" )+" "+
		      score_name ) ;
	      applet.getAppletContext().showDocument( sound, "Arezzo MIDI" ) ;
	      repaint() ;
	    }
	    catch( ConnectException e ) {
	      fail( messages.getString( "Harmony.ServerNotRunning" ), e ) ;
	    }
	    catch( MalformedURLException r ) {
	      fail( messages.getString( "Harmony.CannotFindMIDIFile" ), r ) ;
	    }
	  }
	} ;
    local_listeners.put( ps, olp ) ;
    operation_menu.add( play_op ) ;
    operation_menu.add( "-" ) ;

    MenuItem comment_op = 
      new MenuItem( messages.getString( "Harmony.Comment" )) ;
    comment_op.addActionListener( new ActionListener() {
	public void actionPerformed( ActionEvent e ) {
	  AreaInputContent c = 
	    new AreaInputContent( "Harmony.AddComment",
				  Harmony.me.score.sequence.comments ) {
	      public void callBack() {
		caller.score.sequence.comments = me().name ;
		super.callBack() ;
	      }
	    } ;
	  new HarmonyOkDialog( "Harmony.Comment", c ) ;
	}
      }) ;
    operation_menu.add( comment_op ) ;

    shared_menus.addElement( operation_menu ) ;
    return operation_menu ;
  }

  private Menu newCommunicationsMenu() {
    Menu communication_menu = 
      new Menu( messages.getString( "Harmony.Communications" )) ;
    communication_menu.setEnabled( false );

    Menu mail_menu = new Menu( messages.getString( "Harmony.Mail" )) ;
    mail_menu.setEnabled( client.isConnected()) ;    
    MenuItem mail_score = 
      new MenuItem( messages.getString( "Harmony.Score" )) ;
    mail_score.addActionListener( new ActionListener () {
	private int mail_counter = 0 ;

	public void actionPerformed( ActionEvent e ) {
	  String tmp_mail_suffix = "_"+(mail_counter++)+mail_suffix ;

	  try {// Internationalization lost on new cnpmusic.com site
	    client.saveSequence( score.sequence, 
				 filePrefix()+tmp_mail_suffix ) ;
	    String lang = locale.getLanguage() ;
	    String autoload = "Autoload="+
	      user.getName()+default_separator+score_name+
	      tmp_mail_suffix.substring( 0, 
					 tmp_mail_suffix.length()-
					 notes_suffix.length()) ;
	    MessageFormat mf = 
	      new MessageFormat( messages.getString( "Harmony.Body" )) ;
	    String body = mf.format( new String[] {
	      "http://"+server+"/index2.php?page=arezzo%26"+autoload
	    } ) ;
	    String root = 
	      "http://"+server+"/arezzo/"+
	      lang.substring( 0, 1 ).toUpperCase()+lang.substring( 1 ) ;
	    String u = "User="+user.getName() ;
	    String v = "Email="+user.getEmail() ;
	    String cmd = root+"/frmMail.php"+"?"+body+"&"+u+"&"+v ;
	    URL m = new URL( applet.getDocumentBase(), cmd ) ;
	    applet.getAppletContext().showDocument( m, "Mail Arezzo" ) ;
	  }
	  catch( ConnectException ce ) {
	    fail( messages.getString( "Harmony.ServerNotRunning" ), ce ) ;
	  }
	  catch( Exception ex ) {
	    fail( messages.getString( "Harmony.MailFailed" ), ex ) ;
	  }
	}
      }) ;
    mail_score.setEnabled( false ) ;
    mail_menu.add( mail_score ) ;
    mail_menu.add( "-" ) ;
    MenuItem mail_PS = new MenuItem( "Postscript" ) ;
    mail_PS.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent e ) {
	  try {
	    String s = client.mailUser( "PS", score.abc(), 
					mailSubject( "Harmony.MailPS" , 
						     score_name ),
					score_name );
	    new HarmonyOkDialog( "Harmony.SendingMessage",
				 new MessageContent( s ));
	  }
	  catch( ConnectException ce ) {
	    fail( messages.getString( "Harmony.ServerNotRunning" ), ce ) ;
	  }
	}
      });
    mail_menu.add( mail_PS ) ;
    MenuItem mail_MIDI = new MenuItem( "MIDI" );
    mail_MIDI.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent e) {
	  try {
	    client.saveMIDI( score.sequence, filePrefix()+midi_suffix ) ;
	    String s = client.mailUser( "MIDI", "", 
					mailSubject( "Harmony.MailMIDI" , 
						     score_name ),
					score_name );
	    new HarmonyOkDialog( "Harmony.SendingMessage",
				 new MessageContent( s ));
	  }
	  catch( ConnectException ce ) {
	    fail( messages.getString( "Harmony.ServerNotRunning" ), ce ) ;
	  }
	}
      });
    mail_menu.add( mail_MIDI );
    MenuItem mail_ABC = new MenuItem( "ABC" );
    mail_ABC.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent e) {
	  try {
	    String s = client.mailUser( "ABC", score.abc(), 
					mailSubject( "Harmony.MailABC" , 
						     score_name ),
					score_name );
	    new HarmonyOkDialog( "Harmony.SendingMessage", 
				 new MessageContent( s )) ;
	  }
	  catch( ConnectException ce ) {
	    fail( messages.getString( "Harmony.ServerNotRunning" ), ce ) ;
	  }
	}
      });
    mail_menu.add( mail_ABC );
    communication_menu.add( mail_menu ) ;
    communication_menu.add( "-" ) ;

    Menu share_menu = new Menu( messages.getString( "Harmony.Share" )) ;
    sharing_ops_menus.addElement( share_menu ) ;
    MenuItem share_new = new MenuItem( messages.getString( "Harmony.New" )) ;
    share_new.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent ae ) {
	  LineInputContent snc = 
	    new LineInputContent( "Harmony.NewChannelName",
			      Harmony.me.user.getName()) {
		public void callBack() {
		  caller.shareNewCallBack( me()) ;
		  super.callBack() ;
		}
	      } ;
	  new HarmonyOkDialog( "Harmony.Channel", snc ) ;
	}
      });
    share_menu.add( share_new ) ;
    MenuItem share_join = 
      new MenuItem( messages.getString( "Harmony.Join" )) ;
    share_join.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent ae ) {
	  try {
	    Vector channels = 
	      SharingServer.getChannels( server, 
					 sharing_session_name) ;
	  
	    String m = 
	      Harmony.me.messages.getString( "Harmony.DoubleClickJoin" ) ;
	    ListContent c = new ListContent( channels, m ) {
		public void callBack() {
		  caller.sharingCallBack( me()) ;
		  super.callBack() ;
		}
	      } ;		
	    new HarmonyOkDialog( "Harmony.Channels", c ) ;
	  }
	  catch( Exception e ) {
	    String s = 
	      messages.getString( "Harmony.UnableToListSharedChannels" ) ;
	    MessageContent mc = new MessageContent( s+" "+e );
	    new HarmonyOkDialog( "Harmony.ConnectionError", mc);
	  }
	}
      }) ;
    share_menu.add( share_join ) ;
    communication_menu.add( share_menu ) ;
    MenuItem sync_op = 
      new MenuItem( messages.getString( "Harmony.Synchronize" )) ;
    sharing_ops_menus.addElement( sync_op ) ;
    sync_op.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent ae ) {
	  sharing_listeners.synchronize( ) ;
	}
      }) ;
    communication_menu.add( sync_op ) ;
    MenuItem participants_op = new MenuItem( "Participants" ) ;
    sharing_ops_menus.addElement( participants_op ) ;
    participants_op.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent ae ) {
	  try {
	    String[] p = sharing_listeners.participants( ) ;
	    Vector v = new Vector() ;
	  
	    for( int i = 0 ; i<p.length ; i++ ) {
	      v.addElement( p[i] ) ;
	    }
	    ListContent lc = 
	      new ListContent( v, sharing_listeners.channel.getName()) ;
	    new HarmonyOkDialog( "Harmony.Participants", lc ) ;
	  }
	  catch( Exception e ) {
	    inform( messages.getString( "Harmony.NotSharing" )+" "+e ) ;
	    return ;
	  }
	}
      }) ;
    communication_menu.add( participants_op ) ;
    MenuItem local_op = new MenuItem( "Local" ) ;
    sharing_ops_menus.addElement( local_op ) ;
    local_op.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent ae ) {
	  try {
	    sharing_listeners.disconnect() ;
	  }
	  catch( Exception e ) {
	    inform( messages.getString( "Harmony.NotSharing" )+" "+e ) ;
	    return ;
	  }
	  enableMenus( sharing_ops_menus, false ) ;
	  removeListeners( current_listeners ) ;
	  addListeners( current_listeners = local_listeners ) ;
	  inform( messages.getString( "Harmony.SharingDisabled" )) ;
	}
      }) ;
    communication_menu.add( local_op ) ;
    enableMenus( sharing_ops_menus, false ) ;
    communication_menu.add("-");
    
    Menu chat_menu = new Menu(messages.getString( "Harmony.Chat"));
    MenuItem chat_new = new MenuItem(messages.getString( "Harmony.New"));
    chat_new.addActionListener(new ActionListener () {
	public void actionPerformed(ActionEvent ae) {
	  LineInputContent cnc = 
	    new LineInputContent( "Harmony.NewChannelName" ) {
		public void callBack() {
		  caller.chatNewCallBack( me());
		  super.callBack();
		}
	      } ;
	  new HarmonyOkDialog( "Harmony.Channel", cnc ) ;
	}
      });
    chat_menu.add(chat_new);
    MenuItem chat_join = 
      new MenuItem(messages.getString( "Harmony.Join"));
    chat_join.addActionListener(new ActionListener () {
	public void actionPerformed(ActionEvent ae) {
	  try {
	    Vector channels = 
	      SharingServer.getChannels( server, 
					 chating_session_name) ;
	  
	    String m = 
	      Harmony.me.messages.getString( "Harmony.DoubleClickJoin" ) ;
	    ListContent cfc = new ListContent( channels, m ) {
		public void callBack() {
		  caller.chatCallBack( me());
		  super.callBack();
		}
	      } ;
	    new HarmonyOkDialog( "Harmony.Channels", cfc ) ;
	  }
	  catch( Exception e ) {
	    String s = 
	      messages.getString( "Harmony.UnableToListChatedChannels" ) ;
	    MessageContent mc = new MessageContent( s+" "+e );
	    new HarmonyOkDialog( "Harmony.ConnectionError", mc);
	  }
	}
      });
    chat_menu.add(chat_join);
    communication_menu.add(chat_menu);

    return communication_menu ;
  }

  static OptionListener checks_option_listener ;

  private Menu newPropertiesMenu() {
    Menu properties_menu = 
      new Menu( messages.getString( "Harmony.Properties" )) ;

    Menu tonality_menu = 
      new Menu( messages.getString( "Harmony.Tonality" )) ;
    Menu tonality_mode = 
      new Menu( messages.getString( "Harmony.Mode" )) ;
    MenuItem mode_major = 
      new MenuItem( messages.getString( "Harmony.Major" )) ;
    IndexItemListener mj = 
      new IndexItemListener( Tonality.mode_majeur ) {
      public void actionPerformed( ActionEvent e ) {
	Harmony.me.setTonalityMode( index );
      }
    } ;
    mode_major.addActionListener( mj ) ;
    tonality_mode.add( mode_major ) ;
    MenuItem mode_minor = 
      new MenuItem( messages.getString( "Harmony.Minor" )) ;
    IndexItemListener mn = 
      new IndexItemListener( Tonality.mode_mineur ) {
      public void actionPerformed( ActionEvent e ) {
	Harmony.me.setTonalityMode( index );
      }
    } ;
    mode_minor.addActionListener( mn ) ;
    tonality_mode.add( mode_minor ) ;
    tonality_menu.add( tonality_mode ) ;

    Menu tonality_alteration = 
      new Menu( messages.getString( "Harmony.Alteration" ));
    for(int i = 0 ; i < Alteration.alterations.length ; i++) {
      MenuItem item = new MenuItem(Alteration.alterations[i]);
      IndexItemListener mal = new IndexItemListener(i) {
	public void actionPerformed(ActionEvent e) {
	  Harmony.me.setTonalityAlteration(index);
	}
      } ;
      item.addActionListener(mal);
      tonality_alteration.add(item);
    }
    tonality_menu.add( tonality_alteration ) ;

    Menu tonality_position = 
      new Menu( messages.getString( "Harmony.Note" ));
    for(int i = 0 ; i < Note.noms.length ; i++) {
      MenuItem item = 
	new MenuItem( messages.getString( Note.noms[i] )) ;
      IndexItemListener mal = new IndexItemListener(i) {
	public void actionPerformed(ActionEvent e) {
	  Harmony.me.setTonalityPosition(index);
	}
      } ;
      item.addActionListener(mal);
      tonality_position.add(item);
    }
    tonality_menu.add( tonality_position ) ;
    properties_menu.add( tonality_menu ) ;

    MenuItem mesure_op = 
      new MenuItem( messages.getString( "Harmony.Mesure" )) ;
    mesure_op.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	LineInputContent c = new LineInputContent( "Harmony.NewMesure" ) {
	    public void callBack() {
	      caller.mesureCallBack( me()) ; 
	      super.callBack() ;
	    }
	  } ;
	new HarmonyOkDialog( "Harmony.Mesure", c ) ;
      }
    }) ;
    properties_menu.add( mesure_op ) ;
    properties_menu.add( "-" ) ;

    MenuItem speed_op = 
      new MenuItem( messages.getString( "Harmony.Speed" )) ;
    speed_op.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent e ) {
	  LineInputContent c = 
	    new LineInputContent( "Harmony.SpeedDecrease" ) {
	      public void callBack() {
		caller.speedCallBack( me()) ;
		super.callBack() ;
	      }
	    } ;
	  new HarmonyOkDialog( "Harmony.Speed", c ) ;
	}
    }) ;
    properties_menu.add( speed_op ) ;

    Menu instruments = new Menu( "Instruments" ) ;
    MenuItem mi_piano = new MenuItem( "Piano" ) ;
    mi_piano.addActionListener( new ActionListener() {
	public void actionPerformed( ActionEvent e ) {
	  for( int i = 0 ; i < Chord.notes_per_chord ; i++ ) {
	    instrumentUpdate( i, Chord.midi_GMpiano ) ;
	  }
	}
      }) ;
    instruments.add( mi_piano ) ;
    instruments.add( new MenuItem( "-" )) ;
    final Vector GM_names = new Vector() ;
    for( int i = 0 ; i <= Byte.MAX_VALUE ; i++ ) {
      GM_names.addElement( messages.getString( "GM_Patch_"+(i+1) )) ;
    }
    for( int i = 0 ; i < Chord.notes_per_chord ; i++ ) {
      MenuItem mi = new MenuItem( Chord.VoiceName( i )) ;
      mi.addActionListener( new IndexItemListener (i) {
	  public void actionPerformed( ActionEvent e ) {
	    String s = 
	      Harmony.me.messages.getString( "Harmony.GMChoice" ) ;
	    final int voice = index ;
	    new HarmonyOkDialog( "Harmony.GMInstruments",
				 new ListContent( GM_names, s ) {
				     public void callBack() {
				       if( index >= 0 ) {
					 instrumentUpdate( voice, index ) ;
				       }
				     }
				   }) ;
	  }
	}) ;
      instruments.add( mi ) ;
    }
    properties_menu.add( instruments ) ;
    properties_menu.add( "-" ) ;

    Menu language_menu = 
      new Menu( messages.getString( "Harmony.Language" )) ;
    MenuItem language_en = 
      new MenuItem( messages.getString( "Harmony.English" )) ;
    language_en.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	setLanguage( "en", "US" ) ;
      }
    }) ;
    language_menu.add( language_en ) ;
    MenuItem language_fr = 
      new MenuItem( messages.getString( "Harmony.French" )) ;
    language_fr.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent e ) {
	  setLanguage( "fr", "FR" ) ;
	}
      }) ;
    language_menu.add( language_fr ) ;
    properties_menu.add( language_menu ) ;

    MenuItem display =
      new MenuItem( messages.getString( "Harmony.Display" )) ;
    OptionListener old = 
      new OptionListener( Score.display_options,
			  "Harmony.DisplaySelect",
			  "Harmony.Display" ) ;
    display.addActionListener( old ) ;
    properties_menu.add( display ) ;
    properties_menu.add( "-" ) ;

    MenuItem checks =
      new MenuItem( messages.getString( "Harmony.Checks" )) ;
    OptionListener olc = 
      new OptionListener( Sequence.default_check_options,
			  "Harmony.ChecksSelect",
			  "Harmony.Checks" ) ;
    checks.addActionListener( olc ) ;
    checks_option_listener = olc ;
    properties_menu.add( checks ) ;

    return properties_menu ;
  }

  private Menu newAlterationMenu() {
    Menu alteration_menu = 
      new Menu( messages.getString( "Harmony.Alteration" ));
    for( int i = 0 ; i < Alteration.alterations.length ; i++ ) {
      MenuItem item = new MenuItem( Alteration.alterations[i] );
      String cmd = "setAlteration"+" "+i ;
      item.setActionCommand( cmd ) ;
      local_listeners.put( cmd, new IndexItemListener( i ) {
	  public void actionPerformed( ActionEvent e ) {
	    setAlteration( index ) ;
	  }
	}) ;
      alteration_menu.add( item );
    }
    MenuItem diminue_item = new MenuItem( Alteration.diminue );
    String cmd = "setAlteration"+" "+Alteration.alteration_diminue_index ;
    diminue_item.setActionCommand( cmd ) ;
    IndexItemListener iil = 
      new IndexItemListener( Alteration.alteration_diminue_index ) {
	public void actionPerformed( ActionEvent e ) {
	  setAlteration( index ) ;
	}
      } ;      
    local_listeners.put( cmd, iil ) ;
    alteration_menu.add( diminue_item );

    MenuItem becarre_item = new MenuItem( Alteration.becarre );
    String cmd2 = "setAlteration"+" "+Alteration.alteration_becarre_index ;
    becarre_item.setActionCommand( cmd2 ) ;
    IndexItemListener iil2 = 
      new IndexItemListener( Alteration.alteration_becarre_index ) {
	public void actionPerformed( ActionEvent e ) {
	  setAlteration( index ) ;
	}
      } ;
    local_listeners.put( cmd2, iil2 ) ;
    alteration_menu.add( becarre_item );

    shared_menus.addElement( alteration_menu ) ;
    return alteration_menu ;
  }

  private Menu newInversionMenu() {
    Menu inversion_menu = 
      new Menu( messages.getString( "Harmony.Inversion" ));
    Menu current_menu = inversion_menu ;

    for(int i = 0 ; i < Chiffrage.chiffres_connus.length ; i++) {
      if( i == Chiffrage.septieme ) {
	Menu septieme_menu = new Menu( "7");
	inversion_menu.add( current_menu = septieme_menu ) ;
	shared_menus.addElement( septieme_menu ) ;
      }
      if( i == Chiffrage.dominante_septieme || 
	  i == Chiffrage.septieme_diminuee ) {
	current_menu.add( "-" ) ;
      }
      String cs = "";
      for( int j = Chiffrage.chiffres_connus[i].length-1 ; j>=0 ; j-- ) {
	cs += " " + Chiffrage.chiffres_connus[i][j];
      }
      MenuItem item = new MenuItem(cs);
      String cmd = "setInversion"+" "+i ;
      item.setActionCommand( cmd ) ;
      local_listeners.put( cmd, new IndexItemListener( i ) {
	  public void actionPerformed( ActionEvent e ) {
	    setInversion( index ) ;
	  }
	}) ;
      current_menu.add( item );
    }
    inversion_menu.add( "-" ) ;

    // rajouter setInversion

    MenuItem jazz = new MenuItem( "Jazz" ) ;
    jazz.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent e ) {
	  LineInputContent c = 
	    new LineInputContent( "Harmony.EnterJazz" ) {
	      public void callBack() {
		try {
		  Chiffrage.Jazz j = new Chiffrage.Jazz( me().name ) ;
		  j.setChiffrage( score.sequence.tonality ) ;

		  if( score.selected_chord != null ) {
		    score.selected_chord.setChiffrage( j );
		    repaint() ;
		  }
		  super.callBack() ;
		}
		catch( Chiffrage.JazzException x ) {
		  String s = messages.getString( "Harmony.ImproperJazz" ) ;
		  inform( s+" "+me().name );
		}
		catch( Intervalle.TooComplexException x ) {
		  String s = messages.getString( "Harmony.ComplexJazz" ) ;
		  inform( s+" "+me().name );
		}
		catch( Exception x ) {
		  fail( "Jazz "+me().name, x ) ;
		}
	      }
	    } ;
	  new HarmonyOkDialog( "Harmony.Jazz", c ) ;
	}
      }) ;
    inversion_menu.add( jazz ) ;

    shared_menus.addElement( inversion_menu ) ;
    return inversion_menu ;
  }

  private Menu newDurationMenu() {
    Menu duration_menu = 
      new Menu( messages.getString( "Harmony.Duration" ));
    for(int i = 0 ; i < Note.durees_connues.length ; i++) {
      MenuItem item = 
	new MenuItem( String.valueOf( Note.durees_connues[i] ));
      String cmd = "setDuration"+" "+i ;
      item.setActionCommand( cmd ) ;
      local_listeners.put( cmd, new IndexItemListener( i ) {
	  public void actionPerformed( ActionEvent e ) {
	    setDuration( index ) ;
	  }
	}) ;
      duration_menu.add(item);
    }
    shared_menus.addElement( duration_menu ) ;
    return duration_menu ;
  }

  private class InsertMenu
    extends Menu
  {
    InsertMenu( String s ) {
      super() ;
      setLabel( messages.getString( s )) ;
    }

    public void addMode( String s, int m ) {
      MenuItem item = new MenuItem( messages.getString( s ));
      String cmd = "setInsertMode"+" "+m ;
      item.setActionCommand( cmd ) ;
      local_listeners.put( cmd, new IndexItemListener( m ) {
	  public void actionPerformed( ActionEvent e ) {
	    setInsertMode( index ) ;
	  }
	}) ;
      add( item );
    }
  }

  private Menu newInsertMenu() {
    InsertMenu insert_menu = new InsertMenu( "Harmony.Insert" ) ;
    InsertMenu insert_chord_menu = new InsertMenu( "Harmony.Chord" ) ;

    insert_chord_menu.addMode( "Harmony.InsertBass", Score.insert_chord_bass ) ;
    insert_chord_menu.addMode( "Harmony.InsertFull", Score.insert_chord_full ) ;
    shared_menus.addElement( insert_chord_menu ) ;
    insert_menu.add( insert_chord_menu ) ;
    insert_menu.add( "-" ) ;

    insert_menu.addMode( "Harmony.LowestNote", Score.insert_lowest ) ;
    insert_menu.add( "-" ) ;

    for( int i = 0 ; i < Chord.notes_per_chord ; i++ ) {
      insert_menu.addMode( Chord.Voice_Names[i], i ) ;
    }
    shared_menus.addElement( insert_menu ) ;
    return insert_menu ;
  }

  private Menu newQuestionMenu() {
    Menu question_menu = new Menu( "?" ) ;
    MenuItem word_question = 
      new MenuItem( messages.getString( "Harmony.ExplainWord" )) ;
    word_question.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
 	try {
	    if(  locale.getLanguage().equals( "fr" )) {
		URL u = new URL( "http://www.cnpmusic.com/plus/lexiqueA.php" ) ;
		String s = messages.getString( "Harmony.ExplainWord" ) ;
		applet.getAppletContext().showDocument( u, s );
	    }
	}
	catch(MalformedURLException exc) {
	    fail( messages.getString( "Harmony.CannotFindGlossary" ), exc);
 	}
      }}) ;
    word_question.setEnabled( false ) ;
    question_menu.add( word_question ) ;
    MenuItem manual_question = 
      new MenuItem( messages.getString( "Harmony.Manual" )) ;
    manual_question.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
 	try {
	  String lang = locale.getLanguage() ;
	  URL u = new URL( "http://"+server+"/"+manual_path ) ;
 	  String name = messages.getString( "Harmony.Manual" );
 	  applet.getAppletContext().showDocument( u, name );
 	}
 	catch(MalformedURLException exc) {
 	  fail( messages.getString( "Harmony.CannotFindManual" ), exc);
 	}
      }}) ;
    question_menu.add( manual_question ) ;
    question_menu.add( "-" ) ;

    MenuItem apropos_question = 
      new MenuItem( messages.getString( "Harmony.APropos" )) ;
    apropos_question.addActionListener( new ActionListener () {
      public void actionPerformed( ActionEvent e ) {
	inform( applet.getAppletInfo()) ;
      }
      }) ;
    question_menu.add( apropos_question ) ;
    return question_menu ;
  }
  
  private void createMenuBar() {

    MenuBar menu_bar = new MenuBar() ;

    shared_menus = new Vector() ;
    sharing_ops_menus = new Vector() ;

    menu_bar.add( newFileMenu()) ;
    menu_bar.add( newOperationsMenu()) ;
    menu_bar.add( newCommunicationsMenu()) ;
    menu_bar.add( newPropertiesMenu()) ;
    menu_bar.add( newAlterationMenu());
    menu_bar.add( newInversionMenu());
    menu_bar.add( newDurationMenu());
    menu_bar.add( newInsertMenu());
    menu_bar.setHelpMenu( newQuestionMenu()) ;
    getFrame( this ).setMenuBar( menu_bar ) ;
  }

  private void createInterface() {

    // Bottom of screen: scrollbar + diagnostic

    Panel bottom = new Panel() ;
    bottom.setLayout(new BorderLayout( 15, 15 )) ;

    scroll = new Scrollbar( Scrollbar.HORIZONTAL, 0, 64, 0, 255 ) ;
    scroll.setBackground( cnpmusic_light_color ) ;
    bottom.add( "North", scroll ) ;

    diagnostic = new TextArea( 7, 20 ) ;
    diagnostic.setEditable( true ) ;
    diagnostic.setFont( new Font( "Helvetica", Font.PLAIN, 14 )) ;
    diagnostic.setText("") ;
    diagnostic.setBackground( cnpmusic_light_color ) ;
    bottom.add( "South", diagnostic ) ;

    // Score space in middle

    setLayout(new BorderLayout( 15, 15 )) ;
    setSize( dimension ) ;
    add( "South", bottom ) ;
  }

  String filePrefix( String u ) {
    return data_directory+"/"+u+"/"+score_name ;
  }

  String filePrefix() {
    return filePrefix( user.getName()) ;
  }

  // Questions

  private void performHarmonyQuestion( String what ) {
    if( what.equals( "Harmony.Check" )) {
      performHarmonyCheck() ;
    }
    else {
      performHarmonyAdvise() ;
    }
  }

  private void performMelodyQuestion( String what, int voice ) {
    if( what.equals( "Harmony.Check" )) {
      performMelodyCheck( voice ) ;
    }
    else {
      performMelodyAdvise( voice, false ) ;
    }
  }

  private void performCounterpointQuestion( String what, int[] voices ) {
    if( what.equals( "Harmony.Check" )) {
      performCounterpointCheck( voices ) ;
    }
    else {
      performCounterpointAdvise( voices ) ;
    }
  }
 
 // Checks

  private void performHarmonyCheck() {
    try {      
      score.sequence.harmonyCheck() ;
      throw 
	new CheckException( "", messages.getString( "Harmony.NoErrors" )) ;
    }
    catch( CheckException e ) {
      report( "Harmony.HarmonyCheckDiag", e ) ;
    }
  }

  private void performMelodyCheck( int voice ) {
    try {      
      score.sequence.melodyCheck( voice ) ;
      throw 
	new CheckException( "", messages.getString( "Harmony.NoErrors" )) ;
    }
    catch( CheckException e ) {
      report( "Harmony.MelodyCheckDiag", e ) ;
    }
  }

  private void performCounterpointCheck( int[] voices ) {
    try {
      if( voices.length <= 1 ) {
	String s = messages.getString( "Harmony.NoVoices" ) ;
	throw new CheckException( "", s ) ;
      }
      score.sequence.counterpointCheck( voices ) ;
      throw 
	new CheckException( "", messages.getString( "Harmony.NoErrors" )) ;
    }
    catch( CheckException e ) {
      report( "Harmony.CounterpointCheckDiag", e ) ;
    }
  }

  private void report( String s, CheckException e ) {
    diagnostic.append( messages.getString( s )+" "+e.getMessage()+".\n" ) ;

    if( e.isReportable()) {
      user.setError( e.name ) ;
    }
    repaint() ;
  }

  // Advices

  private void performHarmonyAdvise() {
    try {
      score.sequence.harmonyAdvise( score.selected_chord ) ;
      throw 
	new AdviseException( messages.getString( "Harmony.NoErrors" )) ;
    }
    catch( AdviseException a ) {
      diagnostic.append( messages.getString( "Harmony.AdviseDiag" )+
			 " "+a.getMessage()+".\n" ) ;
      repaint() ;
    }
  }

  private void performMelodyAdvise( int voice, boolean is_silent ) {
    try {      
      score.sequence.melodyAdvise( voice ) ;
      
      if( !is_silent ) {
	throw 
	  new AdviseException( messages.getString( "Harmony.NoErrors" )) ;
      }
    }
    catch( AdviseException e ) {
      diagnostic.append( messages.getString( "Harmony.AdviseDiag" )+
			 " "+e.getMessage()+".\n" ) ;
      repaint() ;
    }
  }

  private void performCounterpointAdvise( int[] voices ) 
  {
    for( int i = 0 ; i<voices.length ; i++ ) {
      performMelodyAdvise( voices[i], i!=voices.length-1 ) ;
    }
  }

  // Language changes

  private void setLanguage( String l, String c ) {
    HarmonyListeners ls = current_listeners ;
    removeListeners( current_listeners ) ;

    locale = new Locale( l, c ) ;
    messages = ResourceBundle.getBundle( messages_file, locale ) ;

    createMenuBar() ;
    addListeners( ls ) ;
    diagnostic.setText( "" ) ;
  }

  // Tonality changes 

  private void setTonalityMode( int i ) {
    finalizeTonality( score.sequence.tonality, i ) ;
  }

  private void setTonalityAlteration( int i ) {
    Tonality t = score.sequence.tonality ;
    Note n = new Note( t.position, t.octave, t.duree, new Alteration( i )) ;
    finalizeTonality( n, t.mode ) ;
  }

  private void setTonalityPosition( int i ) {
    Tonality t = score.sequence.tonality ;
    Note n = new Note( i, t.octave, t.duree, t.alteration ) ;
    finalizeTonality( n, t.mode ) ;
  }

  private void finalizeTonality( Note n, int i ) {
    try {
      Tonality t = new Tonality( n, i ) ;
      
      if( t.isUsable()) {
	score.sequence.tonality = t ;
	score.sequence.setDegres() ;
	inform( messages.getString( "Harmony.NewTonality" )+" "+t.nom()) ;
      }
      else {
	throw new Intervalle.TooComplexException() ;
      }
    }
    catch( Intervalle.TooComplexException e ) {
      MessageFormat mf = 
	new MessageFormat( messages.getString( "Harmony.TooComplex" )) ;
      inform( mf.format( new String[] 
	{n.nom(), score.sequence.tonality.nom()})) ;
    }
    repaint() ;
  }

  // Alteration changes

  protected void setAlteration( int i ) {
    Alteration a = new Alteration( i ) ;
    
   if( score.selected_note != null && i < Alteration.alterations.length ) {
      score.selected_note.setAlteration( a ); 
    }
    else if( score.selected_chiffrage != null ) {
       score.selected_chiffrage.setAlteration( a, score.selected_chiffre ) ;
    }
    else if( score.selected_degre != null && 
	     i < Alteration.alterations.length ) {
      score.selected_degre.setAlteration( a ) ;
    }
    repaint();
  }
  
  // Inversion changes
  
  protected void setInversion( int i ) {
    if( score.selected_chord != null ) {
      score.selected_chord.setChiffrage( i );
      score.selected_chord.setDegre( score.sequence.tonality ) ;
      repaint();
    }
  }

  // Duration changes

  protected void setDuration(int i) {
    int d = Note.durees_connues[i];

    if( score.selected_chord != null ) {
      score.selected_chord.setDuree( d );
    }
    repaint();
  }

  // Insert changes

  void setInsertMode( int i ) {
    score.insert_mode = i;
    repaint();
  }

  // Operations

  public void loadCallBack( ListContent s) {
    if( s.index >=0 ) {
      score_name = (String)score_names.elementAt( s.index ) ;

      try {
	score = 
	  new Score( score_name,
		     client.getSequence( filePrefix()+notes_suffix )) ;
	inform( score_name+" "+
		messages.getString( "Harmony.Loaded" )) ;
      }
      catch( ConnectException e ) {
	fail( messages.getString( "Harmony.ServerNotRunning" ), e ) ;
	score = new Score( score_name, new Sequence()) ;
      }
      repaint() ;
      showComments( score.sequence ) ;
    }
  }

  public void deleteCallBack( ListContent s ) {
    if( s.index >= 0 ) {
      score_name = (String)score_names.elementAt( s.index ) ;

      try {
	client.deleteSequence( filePrefix()+notes_suffix ) ;
      }
      catch( ConnectException e ) {
	fail( messages.getString( "Harmony.ServerNotRunning" ), e ) ;
	return ;
      }
      for( int i=0 ; i < score_names.size() ; i++ ) {
	if( score_names.elementAt( i ).equals( score_name )) {
	  score_names.removeElement( score_names.elementAt( i )) ;
	  break ;
	}
      }
      inform( score_name+" "+messages.getString( "Harmony.Deleted" )) ;
      score_name = score_name_default ;
      score = new Score( score_name, score.sequence ) ;
      repaint() ;
    }
  }

  public void saveAsCallBack( LineInputContent s) {
    String name ;

    try {
      name = cleanString( s.name ) ;
    }
    catch( NoSuchElementException e ) {name = "";}

    if( !name.equals( "" )) {
      for( int i = 0 ; i < score_names.size() ; i++ ) {
	if( score_names.elementAt( i ).equals( name )) {
	  String msg = messages.getString( "Harmony.SaveAsExists" ) ;
	  MessageFormat mf = new MessageFormat( msg ) ;
	  MessageContent mc = 
	    new MessageContent( mf.format( new String[] {name} )) ;
	  new HarmonyOkDialog( "Harmony.SaveAs", mc ) ;
	  return ;
	}
      }
      score_name = name ;
      score_names.addElement( score_name ) ;
      saveSequence( score.sequence, filePrefix()+notes_suffix ) ;
    }
  }

  public void saveSequence( Sequence s, String filename ) {
    try {
      client.saveSequence( s, filename ) ;
      MessageFormat mf =
	new MessageFormat( messages.getString( "Harmony.SaveSequence" )) ;
      inform( mf.format( new String[] {filename, server} )) ;
    }
    catch( ConnectException e ) {
      fail( messages.getString( "Harmony.ServerNotRunning" ), e ) ;
    }
  }

  public void speedCallBack( LineInputContent s ) {
    int tempo ;

    try {
      tempo = Integer.parseInt( s.name ) ;
    }
    catch( NumberFormatException e ) {
      inform( messages.getString( "Harmony.MalformedNumber" )) ;
      return ;
    }
    if( tempo <= 0 ) {
      inform( messages.getString( "Harmony.MalformedTempo" )) ;
      return ;
    }
    Sequence.updateTempo( tempo ) ;
  }

  public void instrumentUpdate( int voice, int instrument ) {
    Sequence.updateInstrument( voice, instrument ) ;
    String s = messages.getString( "Harmony.CurrentGMInstrument" ) ;
    MessageFormat mf = new MessageFormat( s ) ;
    inform( mf.format( new String[] {
      messages.getString( "GM_Patch_"+(instrument+1) ),
      Chord.theVoiceName( voice )
    })) ;
  }

  public void mesureCallBack( LineInputContent s ) {
    Mesure m = Mesure.parse( s.name ) ;

    if( m != null ) {
      score.sequence.mesure = m ;
    }
    repaint() ;
  }
  
  private void 
  sharingConnect( String channel_name, String error_msg, boolean is_new ) 
  {
    try {
      sharing_listeners = 
	new SharingListeners( user, server, channel_name, is_new ) ;
      enableMenus( sharing_ops_menus, true ) ;
      removeListeners( current_listeners ) ;
      addListeners( current_listeners = sharing_listeners ) ;
      MessageFormat mf =
	new MessageFormat( messages.getString( "Harmony.SharingOk" )) ;
      inform( mf.format( new String[] {channel_name, user.getName()})) ;
    }
    catch( Exception e ) {
      MessageContent mc = new MessageContent( error_msg+": "+e );
      new HarmonyOkDialog( "Harmony.ConnectionError", mc );
    }
  }

  public void sharingCallBack( ListContent s ) {
    if( s.index >= 0 ) {
      sharingConnect( (String)s.names.elementAt( s.index ),
		      messages.getString( "Harmony.UnableJoin" ),
		      false ) ;
      repaint() ;
    }
  }

  public void shareNewCallBack( LineInputContent s ) {
    if( !s.name.equals( "" )) {
      MessageFormat mf = 
	new MessageFormat( messages.getString( "Harmony.UnableCreate" )) ;
      sharingConnect( s.name, 
		      mf.format( new String[] {s.name}),
		      true ) ;
      repaint() ;
    }
  }
   
  private void startChat( String channel_name ) {
    MessageFormat mf =
      new MessageFormat( messages.getString( "Harmony.ChatTitle" )) ;
    String title =
      mf.format( new String[] {user.getName(), channel_name}) ;
    Chat chat_box = new Chat( channel_name, title );
    inform( title ) ;
  }

  public void chatCallBack( ListContent c ) {
    if(c.index >= 0) {
      startChat( (String) c.names.elementAt( c.index ));
    }
  }
    
  public void chatNewCallBack( LineInputContent c ) {
    if(!c.name.equals("")) {
      startChat( c.name );
    }
  }

  String mailSubject( String format, String name ) {
    MessageFormat mf = 
      new MessageFormat( Harmony.me.messages.getString( format )) ;
    return mf.format( new String[] {name}) ;
  }
 
  // Utils for strings

  static String separators_with_leading_blank = "(\"'" ;
  static String separators_without_leading_blank = ",.\n)\\/; ?" ;
  static String separators = 
    separators_with_leading_blank+
    separators_without_leading_blank ;
  static char default_separator = 
    separators_without_leading_blank.charAt( 0 ) ;

  private static String cleanString( String s ) 
    throws NoSuchElementException 
  {
    return new StringTokenizer( s, separators ).nextToken() ;
  }

  public static String cleanString( String s, boolean allow_dot ) 
    throws NoSuchElementException 
  {
    String seps = 
      (allow_dot) ? separators : separators.replace( '.', '(' ) ;
    return new StringTokenizer( s, seps ).nextToken() ;
  }

  //

  static Frame getFrame( Component component ) {
    Component c = component;

    if( c instanceof Frame )
      return (Frame)c;
    
    while( (c = c.getParent()) != null ) {
      if( c instanceof Frame )
	return (Frame)c;
    }
    Harmony.fail( "getFrame/Enclosing frame not found" ) ;
    return( null ) ;
  }

  private void showComments( Sequence s ) {
    if( Score.display_options.isTrue( "Score.ShowComments" ) &&
	!s.comments.equals( "" )) {
      new HarmonyOkDialog( "Harmony.Comments",
			   new MessageContent( s.comments )) ;
    }
  }
}

// List of elements of ns.

class ListContent 
  extends java.awt.List
  implements OkDialogContent 
{
  int index = -1 ;
  Harmony caller ;
  Vector names ;

  private String message ;
  
  ListContent( Vector ns, String s ) {
    super( 10, false ) ;
    caller = Harmony.me ;
    names = ns ;
    message = s ;
  }

  // One cannot use "this" in anonymous inheriting classes.
  
  public ListContent me() {
    return this ;
  }

  public Panel createInterface() {
    Panel p = new Panel() ;
    p.setLayout( new BorderLayout( 15, 15 )) ;
    p.add( "North", new Label( message )) ;
    p.add( "Center", this ) ;

    for( int i=0 ; i < names.size() ; i++ ) {
      add( (String)names.elementAt( i )) ;
    }
    return p ;
  }

  public void okAction() {
    index = getSelectedIndex() ;
  }

  public void cancelAction() {}

  public void callBack() {
  }
}

class LineInputContent 
  implements OkDialogContent 
{
  Harmony caller ;
  String message ;

  protected TextField input ;
  String name = "";

  LineInputContent( String s ) {
    this( s, "" ) ;
  }

  LineInputContent( String s, String d ) {
    caller = Harmony.me ;
    message = caller.messages.getString( s ) ;
    name = d ;
  }

  public LineInputContent me() {
    return this ;
  }

  public void okAction() {
    name = input.getText() ;
  }

  public void cancelAction() {}

  public Panel createInterface() {
    Panel p = new Panel() ;
    p.setLayout( new BorderLayout( 7, 15 )) ;
    p.add( "North", new Label( message )) ;
    p.add( "Center", input = new TextField( name, 10 )) ;
    input.selectAll() ;
    return p ;
  }
  
  public void callBack() {
  }
}

class AreaInputContent 
  implements OkDialogContent, Constants
{
  Harmony caller ;
  String message ;

  protected TextArea input ;
  String name = "";

  AreaInputContent( String s ) {
    this( s, "" ) ;
  }

  AreaInputContent( String s, String d ) {
    caller = Harmony.me ;
    message = caller.messages.getString( s ) ;
    name = d ;
  }

  public AreaInputContent me() {
    return this ;
  }

  public void okAction() {
    name = input.getText() ;
  }

  public void cancelAction() {}

  public Panel createInterface() {
    Panel p = new Panel() ;
    p.setLayout( new BorderLayout( 7, 15 )) ;
    p.add( "North", new Label( message )) ;
    p.add( "Center", input = new TextArea( name, 20, 80 )) ;
    input.setBackground( cnpmusic_light_color ) ;
    input.selectAll() ;
    return p ;
  }
  
  public void callBack() {
  }
}

class MessageContent 
  implements OkDialogContent, Constants
{
  String message;

  MessageContent( String msg ) {
    message = msg;
  }

  public Panel createInterface() {
    Panel p = new Panel();
    p.setLayout( new BorderLayout( 3, 7 ));
    Component c = (message.indexOf( "\n" ) >= 0) ?
      (Component)new TextArea( message ) :
      (Component)new Label( message ) ;
    c.setBackground( cnpmusic_light_color ) ;
    p.add( "Center", c );
    return p;
  }

  public void okAction() {}
    
  public void cancelAction() {}
    
  public void callBack() {}
}

class FailMessageContent 
  extends MessageContent
{
  FailMessageContent( String s ) {
    super( s ) ;
  }

  public Panel createInterface() {
    Panel p = super.createInterface() ;
    p.add( "North", 
	   new Label( Harmony.me.messages.getString( "Harmony.Contact" ))) ;
    return p ;
  }
}

