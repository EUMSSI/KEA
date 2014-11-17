package kea.stemmers;


/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Lucene" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Lucene", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

/**
 * A stemmer for German words. The algorithm is based on the report
 * "A Fast and Simple Stemming Algorithm for German Words" by Joerg
 * Caumanns (joerg.caumanns@isst.fhg.de).
 *
 * Changed stem() from protected to public.
 * Changed coding for umlaute to unicode.
 *
 * @author    Gerhard Schwarz
 * @version   $Id: GermanStemmer.java,v 1.1 2004/12/15 01:13:54 mdewsnip Exp $
 */
public class GermanStemmer extends Stemmer
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * Buffer for the terms while stemming them.
     */
    private StringBuffer sb = new StringBuffer();

    /**
     * Indicates if a term is handled as a noun.
     */
    private boolean uppercase = false;

    /**
     * Amount of characters that are removed with <tt>substitute()</tt> while stemming.
     */
    private int substCount = 0;

    /**
     * Stemms the given term to an unique <tt>discriminator</tt>.
     *
     * @param term  The term that should be stemmed.
     * @return      Discriminator for <tt>term</tt>
     */
    public String stem( String term )
    {
	// Mark a possible noun.
	uppercase = Character.isUpperCase( term.charAt( 0 ) );
	// Use lowercase for medium stemming.
	term = term.toLowerCase();
	if ( !isStemmable( term ) )
	    return term;
	// Reset the StringBuffer.
	sb.delete( 0, sb.length() );
	sb.insert( 0, term );
	// Stemming starts here...
	substitute( sb );
	strip( sb );
	optimize( sb );
	resubstitute( sb );
	removeParticleDenotion( sb );
	return sb.toString();
    }

    /**
     * Checks if a term could be stemmed.
     *
     * @return  true if, and only if, the given term consists in letters.
     */
    private boolean isStemmable( String term )
    {
	for ( int c = 0; c < term.length(); c++ ) {
	    if ( !Character.isLetter( term.charAt( c ) ) ) return false;
	}
	return true;
    }

    /**
     * suffix stripping (stemming) on the current term. The stripping is reduced
     * to the seven "base" suffixes "e", "s", "n", "t", "em", "er" and * "nd",
     * from which all regular suffixes are build of. The simplification causes
     * some overstemming, and way more irregular stems, but still provides unique.
     * discriminators in the most of those cases.
     * The algorithm is context free, except of the length restrictions.
     */
    private void strip( StringBuffer buffer )
    {
	boolean doMore = true;
	while ( doMore && buffer.length() > 3 ) {
	    if ( ( buffer.length() + substCount > 5 ) &&
		buffer.substring( buffer.length() - 2, buffer.length() ).equals( "nd" ) )
	    {
		buffer.delete( buffer.length() - 2, buffer.length() );
	    }
	    else if ( ( buffer.length() + substCount > 4 ) &&
		buffer.substring( buffer.length() - 2, buffer.length() ).equals( "em" ) ) {
		buffer.delete( buffer.length() - 2, buffer.length() );
	    }
	    else if ( ( buffer.length() + substCount > 4 ) &&
		buffer.substring( buffer.length() - 2, buffer.length() ).equals( "er" ) ) {
		buffer.delete( buffer.length() - 2, buffer.length() );
	    }
	    else if ( buffer.charAt( buffer.length() - 1 ) == 'e' ) {
		buffer.deleteCharAt( buffer.length() - 1 );
	    }
	    else if ( buffer.charAt( buffer.length() - 1 ) == 's' ) {
		buffer.deleteCharAt( buffer.length() - 1 );
	    }
	    else if ( buffer.charAt( buffer.length() - 1 ) == 'n' ) {
		buffer.deleteCharAt( buffer.length() - 1 );
	    }
	    // "t" occurs only as suffix of verbs.
	    else if ( buffer.charAt( buffer.length() - 1 ) == 't' && !uppercase ) {
		buffer.deleteCharAt( buffer.length() - 1 );
	    }
	    else {
		doMore = false;
	    }
	}
    }

    /**
     * Does some optimizations on the term. This optimisations are
     * contextual.
     */
    private void optimize( StringBuffer buffer )
    {
	// Additional step for female plurals of professions and inhabitants.
	if ( buffer.length() > 5 && buffer.substring( buffer.length() - 5, buffer.length() ).equals( "erin*" ) ) {
	    buffer.deleteCharAt( buffer.length() -1 );
	    strip( buffer );
	}
	// Additional step for irregular plural nouns like "Matrizen -> Matrix".
	if ( buffer.charAt( buffer.length() - 1 ) == ( 'z' ) ) {
	    buffer.setCharAt( buffer.length() - 1, 'x' );
	}
    }

    /**
     * Removes a particle denotion ("ge") from a term.
     */
    private void removeParticleDenotion( StringBuffer buffer )
    {
	if ( buffer.length() > 4 ) {
	    for ( int c = 0; c < buffer.length() - 3; c++ ) {
		if ( buffer.substring( c, c + 4 ).equals( "gege" ) ) {
		    buffer.delete( c, c + 2 );
		    return;
		}
	    }
	}
    }

    /**
     * Do some substitutions for the term to reduce overstemming:
     *
     * - Substitute Umlauts with their corresponding vowel: ae,oe,ue -> a,o,u,
     *   "eszet" is substituted by "ss"
     * - Substitute a second char of a pair of equal characters with
     *   an asterisk: ?? -> ?*
     * - Substitute some common character combinations with a token:
     *   sch/ch/ei/ie/ig/st -> $/c/%/&/#/!
     */
    private void substitute( StringBuffer buffer )
    {
	substCount = 0;
	for ( int c = 0; c < buffer.length(); c++ ) {
	    // Replace the second char of a pair of the equal characters with an asterisk
	    if ( c > 0 && buffer.charAt( c ) == buffer.charAt ( c - 1 )  ) {
		buffer.setCharAt( c, '*' );
	    }
	    // Substitute Umlauts.
	    else if ( buffer.charAt( c ) == '\u00E4' ) {
		buffer.setCharAt( c, 'a' );
	    }
	    else if ( buffer.charAt( c ) == '\u00F6' ) {
		buffer.setCharAt( c, 'o' );
	    }
	    else if ( buffer.charAt( c ) == '\u00FC' ) {
		buffer.setCharAt( c, 'u' );
	    }
	    // Take care that at least one character is left left side from the current one
	    if ( c < buffer.length() - 1 ) {
		if ( buffer.charAt( c ) == '\u00DF' ) {
		    buffer.setCharAt( c, 's' );
		    buffer.insert( c + 1, 's' );
		    substCount++;
		}
		// Masking several common character combinations with an token
		else if ( ( c < buffer.length() - 2 ) && buffer.charAt( c ) == 's' &&
		    buffer.charAt( c + 1 ) == 'c' && buffer.charAt( c + 2 ) == 'h' )
		{
		    buffer.setCharAt( c, '$' );
		    buffer.delete( c + 1, c + 3 );
		    substCount =+ 2;
		}
		else if ( buffer.charAt( c ) == 'c' && buffer.charAt( c + 1 ) == 'h' ) {
		    buffer.setCharAt( c, 'C' );
		    buffer.deleteCharAt( c + 1 );
		    substCount++;
		}
		else if ( buffer.charAt( c ) == 'e' && buffer.charAt( c + 1 ) == 'i' ) {
		    buffer.setCharAt( c, '%' );
		    buffer.deleteCharAt( c + 1 );
		    substCount++;
		}
		else if ( buffer.charAt( c ) == 'i' && buffer.charAt( c + 1 ) == 'e' ) {
		    buffer.setCharAt( c, '&' );
		    buffer.deleteCharAt( c + 1 );
		    substCount++;
		}
		else if ( buffer.charAt( c ) == 'i' && buffer.charAt( c + 1 ) == 'g' ) {
		    buffer.setCharAt( c, '#' );
		    buffer.deleteCharAt( c + 1 );
		    substCount++;
		}
		else if ( buffer.charAt( c ) == 's' && buffer.charAt( c + 1 ) == 't' ) {
		    buffer.setCharAt( c, '!' );
		    buffer.deleteCharAt( c + 1 );
		    substCount++;
		}
	    }
	}
    }

    /**
     * Undoes the changes made by substitute(). That are character pairs and
     * character combinations. Umlauts will remain as their corresponding vowel,
     * as "eszet" remains as "ss".
     */
    private void resubstitute( StringBuffer buffer )
    {
	for ( int c = 0; c < buffer.length(); c++ ) {
	    if ( buffer.charAt( c ) == '*' ) {
		char x = buffer.charAt( c - 1 );
		buffer.setCharAt( c, x );
	    }
	    else if ( buffer.charAt( c ) == '$' ) {
		buffer.setCharAt( c, 's' );
		buffer.insert( c + 1, new char[]{'c', 'h'}, 0, 2 );
	    }
	    else if ( buffer.charAt( c ) == 'C' ) {
		buffer.setCharAt( c, 'c' );
		buffer.insert( c + 1, 'h' );
	    }
	    else if ( buffer.charAt( c ) == '%' ) {
		buffer.setCharAt( c, 'e' );
		buffer.insert( c + 1, 'i' );
	    }
	    else if ( buffer.charAt( c ) == '&' ) {
		buffer.setCharAt( c, 'i' );
		buffer.insert( c + 1, 'e' );
	    }
	    else if ( buffer.charAt( c ) == '#' ) {
		buffer.setCharAt( c, 'i' );
		buffer.insert( c + 1, 'g' );
	    }
	    else if ( buffer.charAt( c ) == '!' ) {
		buffer.setCharAt( c, 's' );
		buffer.insert( c + 1, 't' );
	    }
	}
    }
}
