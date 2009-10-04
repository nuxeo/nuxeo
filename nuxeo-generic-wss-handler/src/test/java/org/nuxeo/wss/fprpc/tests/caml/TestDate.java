package org.nuxeo.wss.fprpc.tests.caml;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import junit.framework.TestCase;

public class TestDate extends TestCase {

    static final SimpleDateFormat HTTP_HEADER_DATE_FORMAT =
        new SimpleDateFormat("dd MMM yyyy HH:mm:ss -0000", Locale.US);


    public void testDate() {



        Calendar date = Calendar.getInstance();


        System.out.print(HTTP_HEADER_DATE_FORMAT.format(date.getTime()));


    }
}
