package java.time.chrono;

import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.List;
import java.util.Locale;

/**
 *
 */
public interface Chronology {

    ValueRange range(ChronoField field);
    ChronoLocalDate date(TemporalAccessor temporal);
    Chronology ofLocale(Locale l);
   
ChronoLocalDateTime<? extends ChronoLocalDate> 	localDateTime(TemporalAccessor temporal);


//   <D extends AbstractChronoLocalDate> ChronoZonedDateTimeImpl<D> ensureChronoZonedDateTime(Temporal temporal);
//ChronoZonedDateTime<? extends ChronoLocalDate> 	zonedDateTime(TemporalAccessor temporal);
// <D extends AbstractChronoLocalDate> ChronoLocalDateTimeImpl<D> 
//        ensureChronoLocalDateTime(Temporal temporal);
        
    public boolean isLeapYear(long aLong);

    public Era eraOf(int get);
    List<Era> eras();
   // public AbstractChronoLocalDate ensureChronoLocalDate(Temporal plus);

    public int compareTo(Chronology chronology);

    public String getId();
    
}
