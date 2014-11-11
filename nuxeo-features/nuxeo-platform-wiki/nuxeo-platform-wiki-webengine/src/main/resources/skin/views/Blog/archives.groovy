

def main () {
    def request = req
    def year = Request.getMappingVar('year') as BigInteger
    def month = Request.getMappingVar('month') as BigInteger
    
    nbdays = NumberOfDaysInMonth(year, month)

    if (month) {
        pquery = "SELECT * FROM Document WHERE (dc:created BETWEEN DATE '${year}-${month}-01' AND DATE '${year}-${month}-${nbdays}') AND (ecm:path STARTSWITH '/')"
        c = new GregorianCalendar(year, month - 1, 1) 
    } else {
        pquery = "SELECT * FROM Document WHERE (dc:created BETWEEN DATE '${year}-01-01' AND DATE '${year}-12-31') AND (ecm:path STARTSWITH '/')"
        c = new GregorianCalendar(year, 0, 1) 
    }
    
    results = Request.query(pquery)
    
    Request.render('Blog/archives.ftl',
                  ['year': year, 'month': month, 'pquery': pquery, 'results': results, 'sdate': c.getTime()])
}

/**
 * return True if the year is a leap year
 */
def IsLeapYear (year) {
    if ((year%4 == 0) && (year%100 != 0) || (year%400 == 0)) {
        return true
    } else {
        return false
    }
}

/**
 * return the number of days in the given month
 */
def NumberOfDaysInMonth (year, month) {
    int m = month
    int y = year
    if (m in [1, 3, 5, 7, 8, 10, 12]) {
        return 31
    } else if (m == 2) {
        return IsLeapYear(y) ? 29 : 28
    } else {
        return 30
    }   
}

main()