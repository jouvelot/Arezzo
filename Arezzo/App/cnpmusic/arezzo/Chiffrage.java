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
import java.io.* ;
import java.util.* ;
import java.lang.* ;
import java.text.* ;

class Chiffrage 
  implements Serializable 
{
  Vector chiffres ;
  Vector alterations ;
  int index ; 

  // Indexes

  static final int octave = 0 ;

  static final int tierce = 1 ;

  static final int quinte = 2 ;
  static final int quinte_et_tierce = 3 ;

  static final int sixte = 4 ;
  static final int sixte_et_tierce = 5 ;

  static final int sixte_et_quarte = 6 ;

  static final int septieme = 7 ;
  static final int septieme_1 = 8 ;
  static final int septieme_2 = 9 ;
  static final int septieme_3 = 10 ;
  static final int septieme_4 = 11 ;
  static final int septieme_5 = 12 ;
  static final int last_septieme = septieme_5 ;

  static final int dominante_septieme = 13 ;
  static final int dominante_septieme_1 = 14 ;
  static final int dominante_septieme_2 = 15 ;
  static final int dominante_septieme_3 = 16 ;

  static final int septieme_diminuee = 17 ;

  // Chiffrages connus par le systeme (plus alterations), par index

  static String[][] chiffres_connus = {
    {"8"},
    {"3"},
    {"5"}, {"3", "5"},
    {"6"}, {"3", "6"},
    {"4", "6"},
    {"7"}, {"5", "6"}, {"3", "4"}, {"2"}, {"3", "7"}, {"5", "7"},
    {"+", "7"}, {"5/", "6"}, {"+6"}, {"+4"},
    {"7/"}
  } ;
  static int max_chiffres = 2 ;

  // Notes d'un chiffrage, e.g. 5/do -> [do,mi,sol]

  private Note[] notes ;
  static final int max_notes = 4 ; // Nbre max de notes par chiffrage

  // Position (attention a ne pas confondre avec la position de la note !)
  // de la fondamentale, tierce, quinte, septieme dans notes[], 
  // par index

  static int[] note_fondamentale = {
    0, 
    0, 
    0, 0, 
    2, 2, 
    1, 
    0, 3, 2, 1, 0, 0,
    0, 3, 2, 1,
    0
  } ;
  static int[] note_tierce = {
    -1, 
    1, 
    1, 1, 
    0, 0, 
    2, 
    1, 0, 3, 2, 1, 1,
    1, 0, 3, 2,
    1
  } ;
  static int[] note_quinte = {
    -1, 
    -1, 
    2, 2, 
    1, 1, 
    0, 
    2, 1, 0, 3, 2, 2,
    2, 1, 0, 3,
    2
  } ;
  static int[] note_septieme = {
    -1, 
    -1, 
    -1, -1, 
    -1, -1, 
    -1, 
    3, 2, 1, 0, 3, 3,
    3, 2, 1, 0,
    3
  } ;

  // Positions des chiffres dans notes[], par index 

  static int[][] chiffres_positions = {
    {0},
    {1},
    {2}, {1, 2},
    {2}, {1, 2},
    {1, 2},
    {3}, {2, 3}, {1, 2}, {1}, {1, 3}, {2, 3},
    {1, 3}, {0, 3}, {2}, {1},
    {3}
  } ;

  // Tons par chiffrage, par index

  static int[][] intervalles = {
    {0},
    {0, 2},
    {0, 2, 2}, {0, 2, 2},
    {0, 2, 3}, {0, 2, 3},
    {0, 3, 2},
    {0, 2, 2, 2}, {0, 2, 2, 1}, {0, 2, 1, 2}, {0, 1, 2, 2}, 
                  {0, 2, 2, 2}, {0, 2, 2, 2},
    {0, 2, 2, 2}, {0, 2, 2, 1}, {0, 2, 1, 2}, {0, 1, 2, 2},
    {0, 2, 2, 2}
  } ;

  static Color color_selected = Color.red ;

  Chiffrage() {
    chiffres = new Vector() ;
    alterations = new Vector() ;
    chiffres.addElement( chiffres_connus[quinte][0] ) ;
    alterations.addElement( Alteration.pas_d_alterations ) ;
    index = quinte ;
  }

  Chiffrage( StringTokenizer st ) {
    try {
      chiffres = new Vector() ;
      alterations = new Vector() ;

      while( st.hasMoreElements()) {
	String s = st.nextToken() ;
	
	if( s.equals( "|" )) {
	  break ;
	}
	chiffres.addElement( s ) ;
	alterations.addElement( new Alteration( st )) ;
      }
    }
    catch( Exception e ) {
      Harmony.fail("Chiffrage - malformed file", e ) ;
    }
    setIndex() ;
  }
    
  protected void initChiffrage( int idx ) {
    index = idx ;
    String[] cs = chiffres_connus[ index ] ;
    chiffres = new Vector( cs.length ) ;
    alterations = new Vector( cs.length ) ;

    for( int i = 0 ; i<cs.length ; i++ ) {
      chiffres.insertElementAt( cs[i], i ) ;
      alterations.insertElementAt( Alteration.pas_d_alterations, i ) ;
    }
  }

  Chiffrage( int idx ) {
    initChiffrage( idx ) ;
  }

  String sprintf() {
    String s = "" ;

    for( int i = 0 ; i<chiffres.size() ; i ++ ) {
      s += (String)chiffres.elementAt( i )+" "+
	  ((Alteration)alterations.elementAt( i )).sprintf()+" " ;
    }
    return s+"|\n" ;
  }

  static class JazzException 
    extends Exception 
  {
    JazzException() {
      super() ;
    }

    JazzException( String s ) {
      super( s ) ;
    }
  } ;

  static class Jazz 
    extends Chiffrage
  {
    Note fondamentale, basse ;
    int[] modes ; // for fundamental (unused), third, fifth, seventh

    private String input ;
    private int input_index ;

    Jazz( String s ) 
      throws JazzException
    {
      super() ;
      index = septieme ;
      modes = new int[] {-1, -1, -1, -1} ;
      input = s ;
      input_index = 0 ;
      parseJazz() ;
    }

    private void parseJazz() 
      throws JazzException
    {
      fondamentale = nextNote() ;
      modes[ note_tierce[ index ]] = Intervalle.major ;

      try {
	nextIntToken( new String[] {"m"} ) ;
	modes[ note_tierce[ index ]] = Intervalle.minor ;
      }
      catch( JazzException e ) {
      }
      try {
	String s = nextStringToken( new String[] {"M", "-", "+", "dim"} ) ;
	modes[ note_septieme[ index ]] = 
	  (s.equals( "M" )) ? Intervalle.major :
	  (s.equals( "+" )) ? Intervalle.augmented :
	  Intervalle.diminished ;
	
	if( s.equals( "dim" )) {
	  modes[ note_tierce[ index ]] = Intervalle.minor ;
	  modes[ note_quinte[ index ]] = Intervalle.diminished ;
	}
      }
      catch( JazzException e ) {
      }
      try {
	nextIntToken( new String[] {"7"} ) ;
	
	if( modes[ note_septieme[ index ]] == -1 ) {
	  modes[ note_septieme[ index ]] = Intervalle.minor ;
	}
	if( modes[ note_quinte[ index ]] == Intervalle.diminished ) {
	  checkNoNextToken() ;
	}
      }
      catch( JazzException e ) {
	if( modes[ note_septieme[ index ]] != -1 ) {
	  throw new JazzException( input_index+"" ) ;
	}
      }
      if( modes[ note_quinte[ index ]] == -1 ) {
	modes[ note_quinte[ index ]] = Intervalle.just ;
      }
      try {
	String q = nextStringToken( new String[] {"b5", "#5"} ) ;
	modes[ note_quinte[ index ]] = (q.equals( "b5" )) ? 
	  Intervalle.diminished : Intervalle.augmented ;
      }
      catch( JazzException e ) {
      }
      basse = fondamentale ;

      try {
	nextIntToken( new String[] {"/"} ) ;

	try{
	  basse = nextNote() ;
	}
	catch( JazzException e ) {
	  basse = null ;
	  throw new JazzException() ;
	}
      }
      catch( JazzException e ) {
	if( basse == null ) {
	  throw new JazzException( input ) ;
	}
      }
      checkNoNextToken() ;
    }

    private void checkNoNextToken() 
      throws JazzException
    {
      if( input_index != input.length()) {
	throw new JazzException( input ) ;
      }
    }

    private int nextIntToken( String[] expected ) 
      throws JazzException
    {
      if( input.startsWith( " " )) {
	input_index++ ;
	return nextIntToken( expected ) ;
      }
      String current_input = input.substring( input_index ) ;
      
      for( int i = 0 ; i<expected.length ; i++ ) {
	if( current_input.startsWith( expected[ i ])) {
	  input_index += expected[ i ].length() ;
	  return i ;
	}
      }
      throw new JazzException( current_input );
    }

    private String nextStringToken( String[] expected ) 
      throws JazzException
    {
      return expected[ nextIntToken( expected )] ;
    }

    private Note nextNote() 
      throws JazzException
    {
      Note n = new Note() ;
      n.position = nextIntToken( Note.LETTERS ) ;
      n.alteration = Alteration.pas_d_alterations ;

      try {
	n.alteration = 
	  new Alteration.Jazz( nextIntToken( Alteration.alterations )) ;
      }
      catch( JazzException e ) {
      }
      return n ;
    }

    void setChiffrage( Tonality tonality ) 
      throws Intervalle.TooComplexException
    {
      super.setNotes( fondamentale, tonality ) ;
      int f = note_fondamentale[ index ] ;
      int t = note_tierce[ index ] ;
      int q = note_quinte[ index ] ;
      int s = note_septieme[ index ] ;

      if( modes[ s ] != -1 ) {
	setChiffrageSeptieme( f, t, q, s ) ;
	return ;
      }
      if( basse == fondamentale ) {
	this.initChiffrage( quinte_et_tierce ) ;
	updateChiffre( f, t, 0 ) ;
	updateChiffre( f, q, 1 ) ;
      }
      else if( super.notes[ t ].hasSameName( basse )) {
	this.initChiffrage( sixte_et_tierce ) ;
	updateChiffre( f, t, 0 ) ;
      }
      else if( super.notes[ q ].hasSameName( basse )) {
	this.initChiffrage( sixte_et_quarte ) ;
	updateChiffre( f, t, 1 ) ;
      }
      else {
	String st = "(Chiffrage/setChiffrage "+f+" "+t+" "+q+" "+s+")" ;
	throw new Intervalle.TooComplexException( st ) ;
      }
    }

    private void setChiffrageSeptieme( int f, int t, int q, int s )
      throws Intervalle.TooComplexException
    {
      if( modes[ t ] == Intervalle.minor && 
	  modes[ q ] == Intervalle.diminished && 
	  modes[ s ] == Intervalle.diminished ) {
	this.initChiffrage( septieme_diminuee ) ;
	return ;
      }
      boolean is_dominant = 
	modes[ t ] == Intervalle.major && 
	modes[ q ] == Intervalle.just && 
	modes[ s ] == Intervalle.minor ;
      int base = (is_dominant) ? dominante_septieme : septieme ;
	
      if( basse == fondamentale ) {
	if( is_dominant ) {
	  this.initChiffrage( base ) ;
	  return ;
	}
	if( needsUpdatedChiffre( f, t )) {
	  if( needsUpdatedChiffre( f, q )) {
	    String st = "(Chiffrage/setChiffrageSeptieme-1 "+
	      f+" "+t+" "+q+" "+s+")" ;
	    throw new Intervalle.TooComplexException( st ) ;
	  }
	  this.initChiffrage( septieme_4 ) ;
	  updateChiffre( f, t, 0 ) ;
	  updateChiffre( f, s, 1 ) ;
	  return ;
	}
	if( needsUpdatedChiffre( f, q ) || 
	    modes[ q ] == Intervalle.diminished ) {
	  this.initChiffrage( septieme_5 ) ;
	  updateChiffre( f, q, 0 ) ;
	  updateChiffre( f, s, 1 ) ;
	  return ;
	}
	this.initChiffrage( base ) ;
	updateChiffre( f, s, 0 ) ;
      }
      else if( super.notes[ t ].hasSameName( basse )) {
	this.initChiffrage( base+(septieme_1-septieme) ) ;

	if( !is_dominant ) {
	  updateChiffre( f, s, 0 ) ;
	}
      }
      else if( super.notes[ q ].hasSameName( basse )) {
	this.initChiffrage( base+(septieme_2-septieme) ) ;

	if( !is_dominant ) {
	  updateChiffre( f, s, 0 ) ;
	}
      }
      else if( super.notes[ s ].hasSameName( basse )) {
	this.initChiffrage( base+(septieme_3-septieme) ) ;
      }
      else {
	String st = "(Chiffrage/setChiffrageSeptieme-2 "+
	  f+" "+t+" "+q+" "+s+")" ;
	throw new Intervalle.TooComplexException( st ) ;
      }
    }

    private boolean needsUpdatedChiffre( int low, int high ) 
      throws Intervalle.TooComplexException
    {
      Intervalle i = new Intervalle( super.notes[ low ], super.notes[ high ] ) ;
      Alteration alt = 
	super.notes[ high ].alteration.add( i.alteration( modes[ high ] )) ;

      return !alt.equals( Alteration.pas_d_alterations ) ;
    }

    private void updateChiffre( int low, int high, int position ) 
      throws Intervalle.TooComplexException
    {
      Intervalle i = new Intervalle( super.notes[ low ], super.notes[ high ] ) ;
      Alteration alt = 
	super.notes[ high ].alteration.add( i.alteration( modes[ high ] )) ;
      alterations.setElementAt( alt, position ) ;
    }
  }      

  static String unknown_jazz = "*" ;
  
  private String jazzTierce( Note f, Note t ) 
    throws JazzException, Intervalle.TooComplexException
  {
    String msg = "Chiffrage/jazzTierce: "+f+" "+t ;
    Intervalle ti = new Intervalle( f, t ) ;
    return 
      (isDominantSeventh() || isDiminishedSeventh()) ? "" :
      (ti.isTierceMajeure()) ? "" : 
      (ti.isTierceMineure()) ? "m" : 
      new Intervalle.TooComplexException( msg ).throwMe() ;
  }

  private String jazzQuinte( Note f, Note q ) 
    throws JazzException, Intervalle.TooComplexException
  {
    String msg = "Chiffrage/jazzQuinte: "+f+" "+q ;
    return
      Intervalle.isQuinteJuste( f, q ) ? "" :
      Intervalle.isDiminished( f, q ) ? "b5" :
      Intervalle.isAugmented( f, q ) ? "#5" :
      new Intervalle.TooComplexException( msg ).throwMe() ;
  }

  private String jazzSeptieme( Note f, Note s ) 
    throws JazzException, Intervalle.TooComplexException
  {
    if( !isSeventh()) {
      return "" ;
    }
    String s_type = "7" ;

    if( isSpeciesSeventh()) {
      String msg = "Chiffrage/jazzSeptieme: "+f+" "+s ;
      Intervalle si = new Intervalle( f, s ) ;
      s_type =  
	(si.isSeptiemeMineure()) ? "7" : 
	(si.isSeptiemeMajeure()) ? "M7" :
	(si.isDiminished( f, s )) ? "-7" :
	(si.isAugmented( f, s )) ? "+7" :
	new Intervalle.TooComplexException( msg ).throwMe() ;
    }
    if( isDiminishedSeventh()) {
      s_type = "dim7" ;
    }
    return s_type ;
  }

  private Note basse( Chord c ) 
    throws JazzException 
  {
    Note b = c.notes[ Chord.basse ] ;

    if( b == null ) {
      if( this instanceof Chiffrage.Jazz ) {
	b = ((Chiffrage.Jazz)this).basse ;
      }
      else {
	throw new JazzException() ;
      }
    }
    return b ;
  }

  private String jazzFundamental( Chord c, Tonality ton ) 
    throws JazzException, Intervalle.TooComplexException
  {
      Note b = basse( c ) ;

      setNotes( b, ton.alteredTonality( c )) ;

      Note f = noteAbove( note_fondamentale[ index ], null ) ;
      Note t = noteAbove( note_tierce[ index ], f ) ;
      Note q = noteAbove( note_quinte[ index ], t ) ;

      String t_type = jazzTierce( f, t ) ;
      String q_type = jazzQuinte( f, q ) ;

      if( t_type.equals( "m" ) && q_type.equals( "b5" )) {
	t_type = "dim" ;
	q_type = "" ;
      }
      String s_type = (isSeventh()) ? 
	jazzSeptieme( f, noteAbove( note_septieme[ index ], q )) :
	"" ;

      return 
	t_type+
	s_type+
	((isDiminishedSeventh()) ? "" : q_type) ;
  }

  public String jazz( Chord c, Tonality ton ) 
  {
    try {
      String s = jazzFundamental( c, ton ) ;

      Note b = basse( c ) ;
      Note f = noteAbove( note_fondamentale[ index ], null ) ;

      return
	f.lettre()+
	jazzFundamental( c, ton )+
	((b.hasSameName( f )) ? "" : "/"+b.lettre())  ;
    }
    catch( JazzException e ) {
      return unknown_jazz ;
    }
    catch( Intervalle.TooComplexException e ) {
      return unknown_jazz ;
    }
  }

// Niko says:
// si la tierce ET la quinte ET la septieme sont dans la tonalite on ne 
//             fait rien, juste le degre I, II, etc...   
// sin UN SEUL des trois est hors des notes de la tonalite on precise TOUT
// avec les notations suivante:
// tierce mineure: m
// tierce majeure: rien
// quinte: rien
// quinte diminuee: b5
// septieme mineure: 7
// septieme majeure: M7
// et donc, X representant un degre quelconque:
// X7        : tierce majeure, septieme mineure
// Xm7     : tierce mineure, septieme mineure
// Xm7b5 : tierce mineure, quinte diminuee, septieme mineure
// XM7     : tierce majeure, septieme majeure

  public String jazzDegrePostfix( Chord c, Tonality ton ) 
  {
    try {
      String j = jazzFundamental( c, ton ) ;

      Note b = basse( c ) ;
      Note f = noteAbove( note_fondamentale[ index ], null ) ;
      Note t = noteAbove( note_tierce[ index ], f ) ;
      Note q = noteAbove( note_quinte[ index ], t ) ;
      Note s = (isSeventh()) ? noteAbove( note_septieme[ index ], q ) : null ;

      boolean is_usual = 
	ton.contains( f ) && ton.contains( t ) && ton.contains( q ) &&
	(s == null || ton.contains( s )) ;
    
      return
	((is_usual) ? "" : j)+
	((b.hasSameName( f )) ? "" : "/"+ton.degre( b ).nom()) ;
    }
    catch( JazzException e ) {
    }
    catch( Intervalle.TooComplexException e ) {
    }
    return "" ;
  }
    
  private Note noteAbove( int offset, Note n )
    throws JazzException
  {
    if( offset == -1 ) {
      throw new JazzException() ;
    }
    Note a = notes[ offset ] ;

    if( n != null && !Intervalle.isAscendant( n, a )) {
      a.setOctave( n.octave+((a.position > n.position) ? 0 : 1 )) ;
    }
    return a ;
  }

  public void setAlteration( Alteration alt, int chiffre ) {
    if( isDominantSeventh() || isDiminishedSeventh()) {
      return ;
    }
    alterations.setElementAt( alt, chiffre ) ;

    if( Constants.debug_Chiffrage ) {
      System.err.println( "Chiffrage/setAlteration: "+sprintf()) ;
    }
  }

  public void save( PrintWriter st ) {
    for( int i = 0 ; i<chiffres.size() ; i ++ ) {
      st.print( (String)chiffres.elementAt( i )+" " ) ;
      ((Alteration)alterations.elementAt( i )).save( st ) ;
      st.print( " " ) ;
    }
    st.println() ;
  }

  // x,y = position en haut et a gauche de la colonne de chiffrage

  public void draw( Graphics g, int x, int y ) {
    Color old_color = g.getColor() ;

    for( int i = chiffres.size()-1 ; 
	 i >= 0  ; 
	 i--, y += Score.inter_chiffres ) {
      Alteration a = (Alteration)alterations.elementAt(i) ;
      String alt = 
	a.equals( Alteration.alteration_diminue ) ?
	Alteration.diminue :
	a.equals( Alteration.alteration_becarre ) ?
	Alteration.becarre :
	a.nom() ;
      
      g.setColor( (this == Harmony.me.score.selected_chiffrage && 
		   i == Harmony.me.score.selected_chiffre) ?
		  color_selected:
		  old_color ) ;
      g.drawString( alt, x-Score.x_offset_alterations*alt.length(), y ) ;
      g.drawString( (String)chiffres.elementAt(i), x, y ) ;
      g.setColor( old_color ) ;
    }
  }

  public int selectedChiffre( int y ) {
    return
      (chiffres_connus[index].length-1)-
      Math.min( (y-Score.top_chiffrage_y)/Score.inter_chiffres,
		chiffres.size()-1) ;
  }
  
  public boolean isQuinte() {
    return index == quinte || index == quinte_et_tierce ;
  }

  public boolean isSixte() {
    return index == sixte || index == sixte_et_tierce ;
  }

  public boolean isSixteEtQuarte() {
    return index == sixte_et_quarte ;
  }

  public boolean isSeventh() {
    return isSpeciesSeventh() || isDominantSeventh() || isDiminishedSeventh() ;
  }

  public boolean isSpeciesSeventh() {
    return septieme <= index && index <= last_septieme ;
  }

  public boolean isDominantSeventh() {
    return dominante_septieme <= index && index <= dominante_septieme_3 ;
  }

  public boolean isDiminishedSeventh() {
    return index == septieme_diminuee ;
  }

  public Note fondamentale( Chord c, Tonality t ) {
    try {
      Note basse = basse( c ) ;
      setNotes( basse, t ) ;
    }
    catch( Intervalle.TooComplexException e ) {
      return null ;
    }
    catch( Chiffrage.JazzException e ) {
      return null ;
    }
    return notes[ note_fondamentale[ index ]] ;
  }

  public int positionSeventh( Note basse, Tonality t ) {
    try {
      setNotes( basse, t ) ;
    }
    catch( Intervalle.TooComplexException e ) {
      return -1 ;
    }
    if( !isSeventh()) {
      Harmony.fail( "positionSeventh: "+index ) ;
    }
    int offset = 
      (isDiminishedSeventh()) ? max_notes-1 :
      dominante_septieme_3-dominante_septieme-
      (index-((isDominantSeventh()) ? dominante_septieme : septieme)) ;
   return notes[ offset ].position ;
  }

  public int positionSixte() {
    if( !isSixte()) {
      Harmony.fail( "sixte/Unexpected index: "+index ) ;
    }
    return notes[ note_fondamentale[ sixte ]].position ;
  }

  public boolean equals( Chiffrage c ) {
    return index == c.index ;
  }

  //

  void check( Chord a, Tonality t, Options opts ) 
    throws CheckException
  {
    Note[] ns = a.notes ;
    
    if( !t.contains( ns[Chord.basse] )) {
      String s = Harmony.me.messages.getString( "Chiffrage.Belong" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      throw 
	new CheckException( "Check.Chiffrage.Belong",
			    mf.format( new Object[] {ns[Chord.basse].nom()})) ;
    }      
    try {
      setNotes( ns[Chord.basse], t ) ;
    }
    catch( Intervalle.TooComplexException e ) {
      String s = 
	Harmony.me.messages.getString( "Chiffrage.Complex" )+
	" (check "+sprintf()+")" ;
      MessageFormat mf = new MessageFormat( s ) ;
      throw new CheckException( "", mf.format( new Object[] {})) ;
    }
    if( opts.isTrue( "Check.Chiffrage.Missing" )) {
    chiffres_dans_ns: 
      for( int i = 1 ; i<intervalles[index].length ; i++ ) {
	for( int j = 0 ; j<ns.length ; j++ ){
	  if( isCompatible( notes[i], ns[j], t )) {
	    continue chiffres_dans_ns ;
	  }
	}
	String s = Harmony.me.messages.getString( "Chiffrage.Missing" ) ;
	MessageFormat mf = new MessageFormat( s ) ;
	throw new CheckException( "Check.Chiffrage.Missing",
				  mf.format( new Object[] {notes[i].nom()})) ;
      }
    }
    if( opts.isTrue( "Check.Chiffrage.Belong" )) {
      for( int j = 0 ; j<ns.length ; j++ ) {
	if( !allows( ns[j], t )) {
	  String s = Harmony.me.messages.getString( "Chiffrage.Belong" ) ;
	  MessageFormat mf = new MessageFormat( s ) ;
	  throw new CheckException( "Check.Chiffrage.Belong",
				    mf.format( new Object[] {ns[j].nom()})) ;
	}
      }
    }
  }

  public boolean allows( Note n, Note b, Tonality t ) {
    try {
      setNotes( b, t ) ;
    }
    catch( Intervalle.TooComplexException e ) {
      return false ;
    }
    return allows( n, t ) ;
  }
    
  //

  private void setNotes( Note basse, Tonality t ) 
    throws Intervalle.TooComplexException
  {
    notes = new Note[ max_notes ] ;
    notes[ 0 ] = (Note)basse.clone() ;
    int o = basse.octave ;

    notes[ 0 ].setOctave( o ) ;

    for( int i = 1 ; i<intervalles[ index ].length ; i++ ) {
      int suiv = notes[ i-1 ].position+intervalles[ index ][ i ] ;
      
      if( suiv >= Note.noms.length ) {
	suiv -= Note.noms.length ;
	o++ ;
      }
      notes[ i ] = (Note)t.gamme[ suiv ].clone() ;
      notes[ i ].setOctave( o ) ;
    }
    if( isDominantSeventh()) {
      setDominantAlterations() ;
    }
    else if( isDiminishedSeventh()) {
      setDiminishedAlterations() ;
    }
    else {
      setAlterations( t ) ;
    }
    if( Constants.debug_Chiffrage ) {
      System.err.println( "Chiffrage/setNotes: " ) ;

      for( int i = 0 ; i<intervalles[index].length ; i++ ) {
	System.err.println( notes[i].nom()) ;
      }
    }
  }

  // Voir Danhauser, p.126

  static Intervalle[] to_tonality = null ;

  Tonality dominantTonality() 
    throws Intervalle.TooComplexException
  {
    if( to_tonality == null ) {
      try {
	to_tonality = new Intervalle[] {
	  new Intervalle( Note.do2, Note.fa2 ),
	  new Intervalle( Note.do2, Note.re2b ),
	  new Intervalle( Note.do2, Note.si2b ),
	  new Intervalle( Note.do2, Note.sol2 )
	} ;
      }
      catch( Intervalle.TooComplexException e ) {
      }
    }
    int renversement = index-dominante_septieme ;
    return new Tonality( notes[0].add( to_tonality[renversement] ),
			 Tonality.mode_majeur ) ;
  }

  private void setDominantAlterations() {
    Tonality dom_t ;

    try {
      dom_t = dominantTonality() ;
    }
    catch( Intervalle.TooComplexException e ) {
      Harmony.fail( "Chiffrage/setDominantAlterations: " ) ;
      return ;
    }
    if( Constants.debug_Chiffrage ) {
      System.err.println( "Chiffrage/setDominantAlterations: t = "+
			  dom_t.nom()) ;
    }
    for( int i = 0 ; i < notes.length ; i++ ) {
      notes[i].setAlteration( dom_t.gamme[notes[i].position].alteration ) ;
    }
  }

  private void setDiminishedAlterations() 
    throws Intervalle.TooComplexException
  {
    int m = new Intervalle( Intervalle.third, Intervalle.minor ).demiTons() ;

    for( int i = 0 ; i < notes.length-1 ; i++ ) {
      int diff = notes[ i+1 ].demiTons()-notes[ i ].demiTons() ;
      Alteration a = notes[ i+1 ].alteration.add( -(diff-m) ) ;
      notes[ i+1 ].setAlteration( a ) ;
    }
  }

  private void setAlterations( Tonality t ) 
    throws Intervalle.TooComplexException
  {
    for( int i = chiffres_connus[index].length-1 ; i>=0 ; i-- ) {
      Note note = notes[ chiffres_positions[ index ][ i ]] ;
      Alteration alt_chiffre = (Alteration)alterations.elementAt( i ) ;
      Alteration alt_note = null ;
      
      if( alt_chiffre.equals( Alteration.pas_d_alterations )) {
	alt_note = t.gamme[ note.position ].alteration ;
      }
      else if( alt_chiffre.equals( Alteration.alteration_diminue )) {
	Tonality t_basse = new Tonality( notes[0], Tonality.mode_mineur ) ;
	alt_note = 
	  t_basse.gamme[ note.position ].alteration.add( -1 ) ;
      }
      else if( alt_chiffre.equals( Alteration.alteration_becarre )) {
	alt_note = Alteration.pas_d_alterations ;
      }
      else {
	alt_note = alt_chiffre ;
      }
      note.setAlteration ( alt_note ) ;
    }
  }

  private boolean allows( Note n, Tonality t ) 
  {
    for( int i = 0 ; i<intervalles[index].length ; i++ ) {
      if( Constants.debug_Chiffrage ) {
	System.err.println( "Chiffrage/allows: "+
			    notes[i].nom()+" "+n.nom()) ;
      }
      if( isCompatible( notes[i], n, t )) {
	return true ;
      }
    }
    return false ;
  }
    
  private void setIndex() {
    index = -1 ;

    for( int i = 0 ; i<chiffres_connus.length ; i++ ) {
      if( chiffres_connus[i].length != chiffres.size()) {
	continue ;
      }
      int j ;
      for( j = 0 ; j < chiffres.size() ; j++ ) {
	if( !chiffres_connus[i][j].equals((String)chiffres.elementAt(j))) {
	  break ;
	}
      }
      if( j == chiffres.size() ) {
	index = i ;
	return ;
      }
    }
    Harmony.fail( "Chiffrage/setIndex: not found "+sprintf()) ;
  }

  private boolean isCompatible( Note n_chiffre, Note n, Tonality t ) 
  {
    if( n_chiffre.position != n.position ) {
      return false ;
    }
    if( n_chiffre.alteration.equals( Alteration.alteration_diminue )) {
      Tonality t_basse ;

      try {
	t_basse = new Tonality( notes[ 0 ], Tonality.mode_mineur ) ;
	Note n_dim = t_basse.gamme[ n.position ] ;
	return n_dim.alteration.add( -1 ).equals( n.alteration ) ;
      }
      catch( Intervalle.TooComplexException e ) {
	return false ;
      }
    }
    return n_chiffre.alteration.equals( n.alteration ) ;
  }
}
 
