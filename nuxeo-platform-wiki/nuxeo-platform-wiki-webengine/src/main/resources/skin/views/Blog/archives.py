request = req
year = Context.getMappingVar('year')
month = Context.getMappingVar('month')

def IsLeapYear(year):
    """Returns 1 if year is a leap year, zero otherwise."""
    if year%4 == 0:
        if year%100 == 0:
            if year%400 == 0:
                return 1
            else:
                return 0
        else:
            return 1
    else:
        return 0

def NumberDaysMonth(year, month):
    """Returns the number of days in the month.
    If any of the arguments is missing (month or year) the current month/year is assumed."""
    m = int(month)
    y = int(year)
    if m == 2:
        if IsLeapYear(y):
            return 29
        else:
            return 28
    elif m in (1, 3, 5, 7, 8, 10, 12):
        return 31
    else:
        return 30


if month != None:
    nbdays = NumberDaysMonth(year, month)
    pquery = "SELECT * FROM Document WHERE (dc:created BETWEEN DATE '%s-%s-01' AND DATE '%s-%s-%s') AND (ecm:path STARTSWITH '/')" % (year, month, year, month, nbdays)
else:
    pquery = "SELECT * FROM Document WHERE (dc:created BETWEEN DATE '%s-01-01' AND DATE '%s-12-31') AND (ecm:path STARTSWITH '/')" % (year, year)

r = Context.query(pquery)

toto = "SELECT * FROM Document"


Context.render('Blog/archives.ftl',
           {'year': year, 'month': month, 'pquery': str(pquery)})
