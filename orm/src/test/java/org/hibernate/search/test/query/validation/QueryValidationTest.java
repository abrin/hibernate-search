/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.validation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.logging.Log;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.builders.NumericRangeQueryBuilder;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchTestBase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class QueryValidationTest extends SearchTestBase {
	private FullTextSession fullTextSession;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		Transaction tx = openSession().beginTransaction();
		getSession().save( new A() );
		tx.commit();
		getSession().close();

		this.fullTextSession = Search.getFullTextSession( openSession() );
	}

	@After
	public void tearDown() throws Exception {
		fullTextSession.close();
		super.tearDown();
	}

	@Test
	public void testTargetStringEncodedFieldWithNumericRangeQueryThrowsException() {
		Query query = NumericFieldUtils.createNumericRangeQuery( "value", 1, 1, true, true );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, A.class );
		try {
			fullTextQuery.list();
			fail();
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000232" ) );
		}
	}

    @Test
    public void testTargetNumericEncodedFieldWithStringQueryThrowsException() {
        TermQuery query = new TermQuery( new Term("value", "bar") );
        FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, B.class );
        try {
            fullTextQuery.list();
            fail();
        }
        catch (SearchException e) {
            assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000233" ) );
        }
    }
    

    @Test
    public void testStringWithNumericRange() {
        TermQuery query = new TermQuery( new Term("text", "bar") );
        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange("value", 1L, 1L, true, true);
        
        BooleanQuery bq = new BooleanQuery();
        bq.add(query, Occur.SHOULD);
        bq.add(nrq, Occur.SHOULD);

        System.out.println("*** bq:" + bq.toString());
        FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( bq, B.class );
        try {
            fullTextQuery.list();
            fail();
        }
        catch (SearchException e) {
            assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000233" ) );
        }
    }
    @Test
    public void testStringWithNumericTerm() {
        TermQuery query = new TermQuery( new Term("text", "bar") );
        TermQuery nq = new TermQuery( new Term("value", "1") );
        
        BooleanQuery bq = new BooleanQuery();
        bq.add(query, Occur.SHOULD);
        bq.add(nq, Occur.SHOULD);

        System.out.println("*** bq:" + bq.toString());
        FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( bq, B.class );
        try {
            fullTextQuery.list();
            fail();
        }
        catch (SearchException e) {
            assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000233" ) );
        }
    }
    
    @Test
    public void testRawLuceneWithNumericRange() {
        try {
            QueryParser parser = new MultiFieldQueryParser(new String[]{"id","value","text"},new StandardAnalyzer());
            Query query = parser.parse("+(value:[1 TO 1] text:test)");
            FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, B.class );
            fullTextQuery.list();
        }
        catch (SearchException e) {
            e.printStackTrace();
            fail("should not have an exception");
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("should not have an exception");
        }
    }

    @Test
    public void testRawLuceneWithNumericValue() {
        try {
            Query query = new MatchAllDocsQuery();
            QueryParser parser = new MultiFieldQueryParser(new String[]{"id","value","num"},new StandardAnalyzer());
            query = parser.parse("+(value:1 text:test)");
            FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, B.class );
            fullTextQuery.list();
        }
        catch (SearchException e) {
            e.printStackTrace();
            fail("should not have an exception");
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("should not have an exception");
        }
    }

   
	@Test
	public void testTargetingNonIndexedEntityThrowsException() {
		TermQuery query = new TermQuery( new Term("foo", "bar") );
		try {
			fullTextSession.createFullTextQuery( query, C.class );
		}
		catch (IllegalArgumentException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000234" ) );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class
		};
	}

	@Entity
	@Indexed
	public static class A {
		@Id
		@GeneratedValue
		private long id;

		@Field
		private String value;
	}

	@Entity
	@Indexed
	public static class B {
		@Id
		@GeneratedValue
		private long id;

		@Field
		private Long value;
		
		@Field
		private String text;
	}

	@Entity
	public static class C {
		@Id
		@GeneratedValue
		private long id;
	}
}


