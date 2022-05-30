package no.digdir.logging.event;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.Externalizable;
import java.time.Instant;

public abstract class EventRecordBase extends SpecificRecordBase
        implements SpecificRecord, Comparable<SpecificRecord>, GenericRecord, Externalizable {

    public abstract CharSequence getApplicationName();

    public abstract void setApplicationName(CharSequence charSequence);

    public abstract CharSequence getApplicationEnvironment();

    public abstract void setApplicationEnvironment(CharSequence charSequence);

    public abstract Instant getEventCreated();

    public abstract void setEventCreated(Instant instant);
}
