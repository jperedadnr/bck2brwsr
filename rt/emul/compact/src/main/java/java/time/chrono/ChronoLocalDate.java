package java.time.chrono;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;

/**
 *
 * @author johan
 */
public interface ChronoLocalDate extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDate>{
        public Era getEra();

        Chronology getChronology();
        long 	toEpochDay();
}
