/*
 * Copyright 2008-2015 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2015 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.ldap.sdk.unboundidds.monitors;



import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.unboundidds.monitors.MonitorMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class defines a monitor entry that provides information about the memory
 * usage for the JVM in which the Directory Server is running.  In particular,
 * it reports information about the memory pools and garbage collectors defined
 * in the JVM.  The information that may be available in the memory usage
 * monitor entry includes:
 * <UL>
 *   <LI>The names of the memory pools that are in use within the JVM.</LI>
 *   <LI>The number of bytes currently used within each memory pool.</LI>
 *   <LI>The number of bytes used within each memory pool after the last
 *       garbage collection.</LI>
 *   <LI>The names of the garbage collectors that are in use within the
 *       JVM.</LI>
 *   <LI>The number of garbage collections performed by each collector.</LI>
 *   <LI>The total duration of all garbage collections performed by each
 *       collector.</LI>
 *   <LI>The average duration of garbage collections performed by each
 *       collector.</LI>
 *   <LI>The duration of the most recent garbage collection performed by each
 *       collector.</LI>
 *   <LI>The amount of non-heap memory consumed by the JVM.</LI>
 *   <LI>The number of detected pauses of various durations detected by the
 *       server.</LI>
 *   <LI>The duration of the longest pause detected by the server.</LI>
 * </UL>
 * The server should present at most one memory usage monitor entry.  It can be
 * retrieved using the {@link MonitorManager#getMemoryUsageMonitorEntry} method.
 * This entry provides specific methods for accessing information about JVM
 * memory usage (e.g., the {@link MemoryUsageMonitorEntry#getMemoryPoolNames}
 * method can be used to retrieve the names of the memory pool).  Alternately,
 * this information may be accessed using the generic API.  See the
 * {@link MonitorManager} class documentation for an example that demonstrates
 * the use of the generic API for accessing monitor data.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class MemoryUsageMonitorEntry
       extends MonitorEntry
{
  /**
   * The structural object class used in memory usage monitor entries.
   */
  static final String MEMORY_USAGE_MONITOR_OC =
       "ds-memory-usage-monitor-entry";



  /**
   * The name of the attribute that holds the duration of the longest detected
   * pause.
   */
  private static final String ATTR_LONGEST_PAUSE_TIME =
       "max-detected-pause-time-millis";



  /**
   * The name of the attribute that holds the amount of non-heap memory used
   * by the JVM.
   */
  private static final String ATTR_NON_HEAP_USED = "non-heap-memory-bytes-used";



  /**
   * The name of the attribute that holds the total amount of memory used by
   * memory consumers.
   */
  private static final String ATTR_TOTAL_CONSUMER_MEMORY =
       "total-bytes-used-by-memory-consumers";



  /**
   * The name of the attribute that holds the percentage of committed tenured
   * memory held by memory consumers.
   */
  private static final String ATTR_TOTAL_CONSUMER_MEMORY_AS_PCT_OF_COMMITTED =
       "memory-consumers-total-as-percent-of-committed-tenured-memory";



  /**
   * The name of the attribute that holds the percentage of maximum allowed
   * tenured memory held by memory consumers.
   */
  private static final String ATTR_TOTAL_CONSUMER_MEMORY_AS_PCT_OF_MAX =
       "memory-consumers-total-as-percent-of-maximum-tenured-memory";



  /**
   * The prefix that will be used for pauses detected by the server.
   */
  private static final String ATTR_PREFIX_DETECTED_PAUSE =
       "detected-pauses-over-";



  /**
   * The suffix that will be used for attributes providing the total collection
   * count for a garbage collector.
   */
  private static final String ATTR_SUFFIX_TOTAL_COLLECTION_COUNT =
       "-total-collection-count";



  /**
   * The suffix that will be used for attributes providing the total collection
   * duration for a garbage collector.
   */
  private static final String ATTR_SUFFIX_TOTAL_COLLECTION_DURATION =
       "-total-collection-duration";



  /**
   * The suffix that will be used for attributes providing the average
   * collection duration for a garbage collector.
   */
  private static final String ATTR_SUFFIX_AVERAGE_COLLECTION_DURATION =
       "-average-collection-duration";



  /**
   * The suffix that will be used for attributes providing the recent collection
   * duration for a garbage collector.
   */
  private static final String ATTR_SUFFIX_RECENT_COLLECTION_DURATION =
       "-recent-collection-duration";



  /**
   * The suffix that will be used for attributes providing the current bytes
   * used in a memory pool.
   */
  private static final String ATTR_SUFFIX_CURRENT_BYTES_USED =
       "-current-bytes-used";



  /**
   * The suffix that will be used for attributes providing the bytes used after
   * the last collection in a memory pool.
   */
  private static final String ATTR_SUFFIX_BYTES_USED_AFTER_LAST_COLLECTION =
       "-bytes-used-after-last-collection";



  /**
   * The name of the property used to provide the numbers of pauses of various
   * durations detected.
   */
  private static final String PROPERTY_DETECTED_PAUSE_COUNTS =
       "detected-pause-counts";



  /**
   * The name of the attribute that holds the maximum amount of memory that may
   * be used by the JVM, in megabytes.
   */
  private static final String ATTR_MAX_RESERVABLE_MEMORY_MB =
       "maxReservableMemoryMB";



  /**
   * The name of the attribute that holds the amount of memory currently
   * allocated for use by the JVM, in megabytes.
   */
  private static final String ATTR_CURRENT_RESERVED_MEMORY_MB =
       "currentReservedMemoryMB";



  /**
   * The name of the attribute that holds the amount of allocated JVM memory
   * which is actually in use.
   */
  private static final String ATTR_USED_MEMORY_MB = "usedReservedMemoryMB";



  /**
   * The name of the attribute that holds the amount of allocated JVM memory
   * that is not currently in use.
   */
  private static final String ATTR_FREE_MEMORY_MB = "freeReservedMemoryMB";



  /**
   * The name of the attribute that holds the percentage of the maximum JVM
   * memory that is actually in use.
   */
  private static final String ATTR_RESERVED_MEMORY_PERCENT_FULL =
       "reservedMemoryPercentFull";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 1924052253885937441L;



  // The list of garbage collectors for which information is available.
  private final List<String> garbageCollectors;

  // The list of memory pools for which information is available.
  private final List<String> memoryPools;

  // The amount of memory that has currently been allocated by the JVM, in
  // megabytes.
  private final Long currentReservedMemoryMB;

  // The amount of allocated JVM memory that is not currently in use, in
  // megabytes.
  private final Long freeReservedMemoryMB;

  // The maximum pause time detected by the JVM.
  private final Long maxDetectedPauseTime;

  // The maximum amount of memory that may be used by the JVM, in megabytes.
  private final Long maxReservableMemoryMB;

  // The amount of non-heap memory consumed by the JVM.
  private final Long nonHeapMemoryUsed;

  // The percentage of committed tenured memory held by consumers.
  private final Long percentOfCommittedTenuredMemory;

  // The percentage of maximum tenured memory held by consumers.
  private final Long percentOfMaxTenuredMemory;

  // The percentage of the maximum JVM memory that is currently in use.
  private final Long reservedMemoryPercentFull;

  // The total amount of memory held by memory consumers.
  private final Long totalBytesHeldByConsumers;

  // The amount of allocated JVM memory that is currently in use, in megabytes.
  private final Long usedReservedMemoryMB;

  // The number of pauses exceeding specified thresholds.
  private final Map<Long,Long> detectedPauses;

  // The list of bytes used after the last collection per memory pool.
  private final Map<String,Long> bytesUsedAfterLastCollectionPerMP;

  // The list of current bytes used per memory pool.
  private final Map<String,Long> currentBytesUsedPerMP;

  // The list of average collection durations per garbage collector.
  private final Map<String,Long> averageCollectionDurationPerGC;

  // The list of recent collection durations per garbage collector.
  private final Map<String,Long> recentCollectionDurationPerGC;

  // The list of total collection counts per garbage collector.
  private final Map<String,Long> totalCollectionCountPerGC;

  // The list of total collection durations per garbage collector.
  private final Map<String,Long> totalCollectionDurationPerGC;



  /**
   * Creates a new memory usage monitor entry from the provided entry.
   *
   * @param  entry  The entry to be parsed as a memory usage monitor entry.  It
   *                must not be {@code null}.
   */
  public MemoryUsageMonitorEntry(final Entry entry)
  {
    super(entry);

    maxDetectedPauseTime            = getLong(ATTR_LONGEST_PAUSE_TIME);
    nonHeapMemoryUsed               = getLong(ATTR_NON_HEAP_USED);
    totalBytesHeldByConsumers       = getLong(ATTR_TOTAL_CONSUMER_MEMORY);
    percentOfCommittedTenuredMemory =
         getLong(ATTR_TOTAL_CONSUMER_MEMORY_AS_PCT_OF_COMMITTED);
    percentOfMaxTenuredMemory =
         getLong(ATTR_TOTAL_CONSUMER_MEMORY_AS_PCT_OF_MAX);

    maxReservableMemoryMB     = getLong(ATTR_MAX_RESERVABLE_MEMORY_MB);
    currentReservedMemoryMB   = getLong(ATTR_CURRENT_RESERVED_MEMORY_MB);
    usedReservedMemoryMB      = getLong(ATTR_USED_MEMORY_MB);
    freeReservedMemoryMB      = getLong(ATTR_FREE_MEMORY_MB);
    reservedMemoryPercentFull = getLong(ATTR_RESERVED_MEMORY_PERCENT_FULL);


    final TreeMap<Long,Long> pauses = new TreeMap<Long,Long>();

    final TreeSet<String> mpNames = new TreeSet<String>();
    final TreeSet<String> gcNames = new TreeSet<String>();

    final TreeMap<String,Long> averageDurations = new TreeMap<String,Long>();
    final TreeMap<String,Long> currentBytesUsed = new TreeMap<String,Long>();
    final TreeMap<String,Long> lastBytesUsed    = new TreeMap<String,Long>();
    final TreeMap<String,Long> recentDurations  = new TreeMap<String,Long>();
    final TreeMap<String,Long> totalCounts      = new TreeMap<String,Long>();
    final TreeMap<String,Long> totalDurations   = new TreeMap<String,Long>();

    for (final Attribute a : entry.getAttributes())
    {
      final String name      = a.getName();
      final String lowerName = toLowerCase(name);

      if (lowerName.startsWith(ATTR_PREFIX_DETECTED_PAUSE))
      {
        final Long l = getLong(name);

        final String timeStr =
             lowerName.substring(ATTR_PREFIX_DETECTED_PAUSE.length());
        if (timeStr.endsWith("ms"))
        {
          try
          {
            final long millis =
                 Long.parseLong(timeStr.substring(0, timeStr.length()-2));
            pauses.put(millis, l);
          }
          catch (Exception e)
          {
            debugException(e);
          }
        }
        else if (timeStr.endsWith("s"))
        {
          try
          {
            final long millis = 1000 *
                 Long.parseLong(timeStr.substring(0, timeStr.length()-1));
            pauses.put(millis, l);
          }
          catch (Exception e)
          {
            debugException(e);
          }
        }
      }

      int pos = lowerName.indexOf(ATTR_SUFFIX_AVERAGE_COLLECTION_DURATION);
      if (pos > 0)
      {
        final String gcName = name.substring(0, pos);
        gcNames.add(gcName);

        final Long l = getLong(name);
        if (l != null)
        {
          averageDurations.put(toLowerCase(gcName), l);
        }

        continue;
      }

      pos = lowerName.indexOf(ATTR_SUFFIX_BYTES_USED_AFTER_LAST_COLLECTION);
      if (pos > 0)
      {
        final String mpName = name.substring(0, pos);
        mpNames.add(mpName);

        final Long l = getLong(name);
        if (l != null)
        {
          lastBytesUsed.put(toLowerCase(mpName), l);
        }

        continue;
      }

      pos = lowerName.indexOf(ATTR_SUFFIX_CURRENT_BYTES_USED);
      if (pos > 0)
      {
        final String mpName = name.substring(0, pos);
        mpNames.add(mpName);

        final Long l = getLong(name);
        if (l != null)
        {
          currentBytesUsed.put(toLowerCase(mpName), l);
        }

        continue;
      }

      pos = lowerName.indexOf(ATTR_SUFFIX_RECENT_COLLECTION_DURATION);
      if (pos > 0)
      {
        final String gcName = name.substring(0, pos);
        gcNames.add(gcName);

        final Long l = getLong(name);
        if (l != null)
        {
          recentDurations.put(toLowerCase(gcName), l);
        }

        continue;
      }

      pos = lowerName.indexOf(ATTR_SUFFIX_TOTAL_COLLECTION_COUNT);
      if ((pos > 0) && (! lowerName.startsWith("mem-pool-")))
      {
        final String gcName = name.substring(0, pos);
        gcNames.add(gcName);

        final Long l = getLong(name);
        if (l != null)
        {
          totalCounts.put(toLowerCase(gcName), l);
        }

        continue;
      }

      pos = lowerName.indexOf(ATTR_SUFFIX_TOTAL_COLLECTION_DURATION);
      if (pos > 0)
      {
        final String gcName = name.substring(0, pos);
        gcNames.add(gcName);

        final Long l = getLong(name);
        if (l != null)
        {
          totalDurations.put(toLowerCase(gcName), l);
        }

        continue;
      }
    }


    garbageCollectors =
         Collections.unmodifiableList(new ArrayList<String>(gcNames));

    memoryPools = Collections.unmodifiableList(new ArrayList<String>(mpNames));

    totalCollectionCountPerGC = Collections.unmodifiableMap(totalCounts);

    totalCollectionDurationPerGC = Collections.unmodifiableMap(totalDurations);

    averageCollectionDurationPerGC =
         Collections.unmodifiableMap(averageDurations);

    recentCollectionDurationPerGC =
         Collections.unmodifiableMap(recentDurations);

    bytesUsedAfterLastCollectionPerMP =
         Collections.unmodifiableMap(lastBytesUsed);

    currentBytesUsedPerMP = Collections.unmodifiableMap(currentBytesUsed);

    detectedPauses = Collections.unmodifiableMap(pauses);
  }



  /**
   * Retrieves the maximum amount of memory (in megabytes) that may be allocated
   * and used by the JVM.
   *
   * @return  The maximum amount of memory (in megabytes) that may be allocated
   *          and used by the JVM, or {@code null} if this was not included in
   *          the monitor entry.
   */
  public Long getMaxReservableMemoryMB()
  {
    return maxReservableMemoryMB;
  }



  /**
   * Retrieves the amount of memory (in megabytes) that is currently allocated
   * for use by the JVM.
   *
   * @return  The amount of memory (in megabytes) that is currently allocated
   *          for use by the JVM, or {@code null} if this was not included in
   *          the monitor entry.
   */
  public Long getCurrentReservedMemoryMB()
  {
    return currentReservedMemoryMB;
  }



  /**
   * Retrieves the amount of memory (in megabytes) allocated for use by the JVM
   * that is currently in use for holding Java objects.
   *
   * @return  The amount of memory (in megabytes) allocated for use by the JVM
   *          that is currently in use for holding Java objects, or {@code null}
   *          if this was not included in the monitor entry.
   */
  public Long getUsedReservedMemoryMB()
  {
    return usedReservedMemoryMB;
  }



  /**
   * Retrieves the amount of memory (in megabytes) allocated for use by the JVM
   * that is not currently in use for holding Java objects.
   *
   * @return  The amount of memory (in megabytes) allocated for use by the JVM
   *          that is not currently in use for holding Java objects, or
   *          {@code null} if this was not included in the monitor entry.
   */
  public Long getFreeReservedMemoryMB()
  {
    return freeReservedMemoryMB;
  }



  /**
   * Retrieves the percent of the currently-reserved memory that is actually in
   * use by the JVM for storing Java objects.
   *
   * @return  The percent of the currently-reserved memory that is actually in
   *          use by the JVM for storing Java objects.
   */
  public Long getReservedMemoryPercentFull()
  {
    return reservedMemoryPercentFull;
  }



  /**
   * Retrieves the names of the garbage collectors for which information is
   * available.
   *
   * @return  The names of the garbage collectors for which information is
   *          available.
   */
  public List<String> getGarbageCollectorNames()
  {
    return garbageCollectors;
  }



  /**
   * Retrieves the names of the memory pools for which information is available.
   *
   * @return  The names of the memory pools for which information is available.
   */
  public List<String> getMemoryPoolNames()
  {
    return memoryPools;
  }



  /**
   * Retrieves a map containing the total number of garbage collections
   * performed per collector.
   *
   * @return  A map containing the total number of garbage collections performed
   *          per collector.
   */
  public Map<String,Long> getTotalCollectionCounts()
  {
    return totalCollectionCountPerGC;
  }



  /**
   * Retrieves the total number of garbage collections performed by the
   * specified collector.
   *
   * @param  collectorName  The name of the garbage collector for which to
   *                        retrieve the information.
   *
   * @return  The total number of garbage collections performed by the specified
   *          collector, or {@code null} if that information is not available.
   */
  public Long getTotalCollectionCount(final String collectorName)
  {
    return totalCollectionCountPerGC.get(toLowerCase(collectorName));
  }



  /**
   * Retrieves a map containing the total length of time (in milliseconds) spent
   * performing garbage collection per collector.
   *
   * @return  A map containing the total length of time (in milliseconds) spent
   *          performing garbage collection per collector.
   */
  public Map<String,Long> getTotalCollectionDurations()
  {
    return totalCollectionDurationPerGC;
  }



  /**
   * Retrieves the total length of time (in milliseconds) spent performing
   * garbage collection for the specified collector.
   *
   * @param  collectorName  The name of the garbage collector for which to
   *                        retrieve the information.
   *
   * @return  The total length of time (in milliseconds) spent performing
   *          garbage collection for the specified collector, or {@code null} if
   *          that information is not available.
   */
  public Long getTotalCollectionDuration(final String collectorName)
  {
    return totalCollectionDurationPerGC.get(toLowerCase(collectorName));
  }



  /**
   * Retrieves a map containing the average garbage collection duration (in
   * milliseconds) per garbage collector.
   *
   * @return  A map containing the average garbage collection duration (in
   *          milliseconds) per garbage collector.
   */
  public Map<String,Long> getAverageCollectionDurations()
  {
    return averageCollectionDurationPerGC;
  }



  /**
   * Retrieves the average garbage collection duration (in milliseconds) for the
   * specified collector.
   *
   * @param  collectorName  The name of the garbage collector for which to
   *                        retrieve the information.
   *
   * @return  The average garbage collection duration (in milliseconds) for the
   *          specified collector, or {@code null} if that information is not
   *          available.
   */
  public Long getAverageCollectionDuration(final String collectorName)
  {
    return averageCollectionDurationPerGC.get(toLowerCase(collectorName));
  }



  /**
   * Retrieves a map containing the most recent garbage collection duration (in
   * milliseconds) per garbage collector.
   *
   * @return  A map containing the duration of the most recent garbage
   *          collection duration (in milliseconds) per garbage collector.
   */
  public Map<String,Long> getRecentCollectionDurations()
  {
    return recentCollectionDurationPerGC;
  }



  /**
   * Retrieves the duration (in milliseconds) of the most recent garbage
   * collection for the specified collector.
   *
   * @param  collectorName  The name of the garbage collector for which to
   *                        retrieve the information.
   *
   * @return  The duration (in milliseconds) of the most recent garbage
   *          collection for the specified collector, or {@code null} if that
   *          information is not available.
   */
  public Long getRecentCollectionDuration(final String collectorName)
  {
    return recentCollectionDurationPerGC.get(toLowerCase(collectorName));
  }



  /**
   * Retrieves a map containing the current number of bytes used per memory
   * pool.
   *
   * @return  A map containing the current number of bytes used per memory pool.
   */
  public Map<String,Long> getCurrentBytesUsed()
  {
    return currentBytesUsedPerMP;
  }



  /**
   * Retrieves the current number of bytes used for the specified memory pool.
   *
   * @param  poolName  The name of the memory pool for which to retrieve the
   *                   information.
   *
   * @return  The current number of bytes used for the specified memory pool, or
   *          {@code null} if that information is not available.
   */
  public Long getCurrentBytesUsed(final String poolName)
  {
    return currentBytesUsedPerMP.get(toLowerCase(poolName));
  }



  /**
   * Retrieves a map containing the number of bytes used after the last garbage
   * collection per memory pool.
   *
   * @return  A map containing the number of bytes used after the last garbage
   *          collection per memory pool.
   */
  public Map<String,Long> getBytesUsedAfterLastCollection()
  {
    return bytesUsedAfterLastCollectionPerMP;
  }



  /**
   * Retrieves the number of bytes used after the last garbage collection for
   * the specified memory pool.
   *
   * @param  poolName  The name of the memory pool for which to retrieve the
   *                   information.
   *
   * @return  The number of bytes used after the last garbage collection for the
   *          specified memory pool, or {@code null} if that information is not
   *          available.
   */
  public Long getBytesUsedAfterLastCollection(final String poolName)
  {
    return bytesUsedAfterLastCollectionPerMP.get(toLowerCase(poolName));
  }



  /**
   * Retrieves the amount of non-heap memory consumed by the JVM.
   *
   * @return  The amount of non-heap memory consumed by the JVM, or {@code null}
   *          if that information is not available.
   */
  public Long getNonHeapMemoryBytesUsed()
  {
    return nonHeapMemoryUsed;
  }



  /**
   * Retrieves the total amount of memory in bytes held by memory consumers.
   *
   * @return  The total amount of memory in bytes held by memory consumers, or
   *          {@code null} if that information is not available.
   */
  public Long getTotalBytesUsedByMemoryConsumers()
  {
    return totalBytesHeldByConsumers;
  }



  /**
   * Retrieves the percentage of the maximum allowed amount of tenured memory
   * that is used by memory consumers (assuming that all memory used by memory
   * consumers is contained in the tenured generation).
   *
   * @return  The percentage of the maximum allowed amount of tenured memory
   *          that is used by memory consumers, or {@code null} if that
   *          information is not available.
   */
  public Long getPercentageOfMaximumTenuredMemoryUsedByMemoryConsumers()
  {
    return percentOfMaxTenuredMemory;
  }



  /**
   * Retrieves the percentage of the committed amount of tenured memory that is
   * used by memory consumers (assuming that all memory used by memory consumers
   * is contained in the tenured generation).
   *
   * @return  The percentage of the committed amount of tenured memory that is
   *          used by memory consumers, or {@code null} if that information is
   *          not available.
   */
  public Long getPercentageOfCommittedTenuredMemoryUsedByMemoryConsumers()
  {
    return percentOfCommittedTenuredMemory;
  }



  /**
   * Retrieves the number of pauses of various durations detected by the server.
   * The value returned will contain a map between the minimum duration in
   * milliseconds for the associated bucket and the number of pauses detected of
   * at least that duration.
   *
   * @return  The number of pauses of various durations detected by the server.
   */
  public Map<Long,Long> getDetectedPauseCounts()
  {
    return detectedPauses;
  }



  /**
   * Retrieves the duration of the longest pause detected by the server.
   *
   * @return  The duration of the longest pause detected by the server, or
   *          {@code null} if that information is not available.
   */
  public Long getMaxDetectedPauseTimeMillis()
  {
    return maxDetectedPauseTime;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getMonitorDisplayName()
  {
    return INFO_MEMORY_USAGE_MONITOR_DISPNAME.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getMonitorDescription()
  {
    return INFO_MEMORY_USAGE_MONITOR_DESC.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public Map<String,MonitorAttribute> getMonitorAttributes()
  {
    final LinkedHashMap<String,MonitorAttribute> attrs =
         new LinkedHashMap<String,MonitorAttribute>();

    if (maxReservableMemoryMB != null)
    {
      addMonitorAttribute(attrs,
           ATTR_MAX_RESERVABLE_MEMORY_MB,
           INFO_MEMORY_USAGE_DISPNAME_MAX_MEM.get(),
           INFO_MEMORY_USAGE_DESC_MAX_MEM.get(),
           maxReservableMemoryMB);
    }

    if (currentReservedMemoryMB != null)
    {
      addMonitorAttribute(attrs,
           ATTR_CURRENT_RESERVED_MEMORY_MB,
           INFO_MEMORY_USAGE_DISPNAME_CURRENT_MEM.get(),
           INFO_MEMORY_USAGE_DESC_CURRENT_MEM.get(),
           currentReservedMemoryMB);
    }

    if (usedReservedMemoryMB != null)
    {
      addMonitorAttribute(attrs,
           ATTR_USED_MEMORY_MB,
           INFO_MEMORY_USAGE_DISPNAME_USED_MEM.get(),
           INFO_MEMORY_USAGE_DESC_USED_MEM.get(),
           usedReservedMemoryMB);
    }

    if (freeReservedMemoryMB != null)
    {
      addMonitorAttribute(attrs,
           ATTR_FREE_MEMORY_MB,
           INFO_MEMORY_USAGE_DISPNAME_FREE_MEM.get(),
           INFO_MEMORY_USAGE_DESC_FREE_MEM.get(),
           freeReservedMemoryMB);
    }

    if (reservedMemoryPercentFull != null)
    {
      addMonitorAttribute(attrs,
           ATTR_RESERVED_MEMORY_PERCENT_FULL,
           INFO_MEMORY_USAGE_DISPNAME_RESERVED_PCT.get(),
           INFO_MEMORY_USAGE_DESC_RESERVED_PCT.get(),
           reservedMemoryPercentFull);
    }

    if (! garbageCollectors.isEmpty())
    {
      addMonitorAttribute(attrs,
           "gcNames",
           INFO_MEMORY_USAGE_DISPNAME_GC_NAMES.get(),
           INFO_MEMORY_USAGE_DESC_GC_NAMES.get(),
           garbageCollectors);
    }

    if (! totalCollectionCountPerGC.isEmpty())
    {
      for (final String name : totalCollectionCountPerGC.keySet())
      {
        addMonitorAttribute(attrs,
            "totalCollectionCount-" + name,
            INFO_MEMORY_USAGE_DISPNAME_TOTAL_COLLECTION_COUNT.get(name),
            INFO_MEMORY_USAGE_DESC_TOTAL_COLLECTION_COUNT.get(name),
            totalCollectionCountPerGC.get(name));
      }
    }

    if (! totalCollectionDurationPerGC.isEmpty())
    {
      for (final String name : totalCollectionDurationPerGC.keySet())
      {
        addMonitorAttribute(attrs,
            "totalCollectionDuration-" + name,
            INFO_MEMORY_USAGE_DISPNAME_TOTAL_COLLECTION_DURATION.get(name),
            INFO_MEMORY_USAGE_DESC_TOTAL_COLLECTION_DURATION.get(name),
            totalCollectionDurationPerGC.get(name));
      }
    }

    if (! averageCollectionDurationPerGC.isEmpty())
    {
      for (final String name : averageCollectionDurationPerGC.keySet())
      {
        addMonitorAttribute(attrs,
            "averageCollectionDuration-" + name,
            INFO_MEMORY_USAGE_DISPNAME_AVERAGE_COLLECTION_DURATION.get(name),
            INFO_MEMORY_USAGE_DESC_AVERAGE_COLLECTION_DURATION.get(name),
            averageCollectionDurationPerGC.get(name));
      }
    }

    if (! recentCollectionDurationPerGC.isEmpty())
    {
      for (final String name : recentCollectionDurationPerGC.keySet())
      {
        addMonitorAttribute(attrs,
            "recentCollectionDuration-" + name,
            INFO_MEMORY_USAGE_DISPNAME_RECENT_COLLECTION_DURATION.get(name),
            INFO_MEMORY_USAGE_DESC_RECENT_COLLECTION_DURATION.get(name),
            recentCollectionDurationPerGC.get(name));
      }
    }

    if (! memoryPools.isEmpty())
    {
      addMonitorAttribute(attrs,
           "memoryPools",
           INFO_MEMORY_USAGE_DISPNAME_MEMORY_POOLS.get(),
           INFO_MEMORY_USAGE_DESC_MEMORY_POOLS.get(),
           memoryPools);
    }

    if (! currentBytesUsedPerMP.isEmpty())
    {
      for (final String name : currentBytesUsedPerMP.keySet())
      {
        addMonitorAttribute(attrs,
            "currentBytesUsed-" + name,
            INFO_MEMORY_USAGE_DISPNAME_CURRENT_BYTES_USED.get(name),
            INFO_MEMORY_USAGE_DESC_CURRENT_BYTES_USED.get(name),
            currentBytesUsedPerMP.get(name));
      }
    }

    if (! bytesUsedAfterLastCollectionPerMP.isEmpty())
    {
      for (final String name : bytesUsedAfterLastCollectionPerMP.keySet())
      {
        addMonitorAttribute(attrs,
            "bytesUsedAfterLastCollection-" + name,
            INFO_MEMORY_USAGE_DISPNAME_BYTES_USED_AFTER_COLLECTION.get(name),
            INFO_MEMORY_USAGE_DESC_BYTES_USED_AFTER_COLLECTION.get(name),
            bytesUsedAfterLastCollectionPerMP.get(name));
      }
    }

    if (nonHeapMemoryUsed != null)
    {
      addMonitorAttribute(attrs,
           ATTR_NON_HEAP_USED,
           INFO_MEMORY_USAGE_DISPNAME_NON_HEAP_MEMORY.get(),
           INFO_MEMORY_USAGE_DESC_NON_HEAP_MEMORY.get(),
           nonHeapMemoryUsed);
    }

    if (totalBytesHeldByConsumers != null)
    {
      addMonitorAttribute(attrs,
           ATTR_TOTAL_CONSUMER_MEMORY,
           INFO_MEMORY_USAGE_DISPNAME_TOTAL_CONSUMER_MEMORY.get(),
           INFO_MEMORY_USAGE_DESC_TOTAL_CONSUMER_MEMORY.get(),
           totalBytesHeldByConsumers);
    }

    if (percentOfMaxTenuredMemory != null)
    {
      addMonitorAttribute(attrs,
           ATTR_TOTAL_CONSUMER_MEMORY_AS_PCT_OF_MAX,
           INFO_MEMORY_USAGE_DISPNAME_CONSUMERS_AS_PCT_OF_MAX.get(),
           INFO_MEMORY_USAGE_DESC_CONSUMERS_AS_PCT_OF_MAX.get(),
           percentOfMaxTenuredMemory);
    }

    if (percentOfCommittedTenuredMemory != null)
    {
      addMonitorAttribute(attrs,
           ATTR_TOTAL_CONSUMER_MEMORY_AS_PCT_OF_COMMITTED,
           INFO_MEMORY_USAGE_DISPNAME_CONSUMERS_AS_PCT_OF_COMMITTED.get(),
           INFO_MEMORY_USAGE_DESC_CONSUMERS_AS_PCT_OF_COMMITTED.get(),
           percentOfCommittedTenuredMemory);
    }

    if (! detectedPauses.isEmpty())
    {
      final ArrayList<String> values =
           new ArrayList<String>(detectedPauses.size());
      for (final Map.Entry<Long,Long> e : detectedPauses.entrySet())
      {
        values.add(e.getKey() + "ms=" + e.getValue());
      }

      addMonitorAttribute(attrs,
           PROPERTY_DETECTED_PAUSE_COUNTS,
           INFO_MEMORY_USAGE_DISPNAME_DETECTED_PAUSES.get(),
           INFO_MEMORY_USAGE_DESC_DETECTED_PAUSES.get(),
           values);
    }

    if (maxDetectedPauseTime != null)
    {
      addMonitorAttribute(attrs,
           ATTR_LONGEST_PAUSE_TIME,
           INFO_MEMORY_USAGE_DISPNAME_MAX_PAUSE_TIME.get(),
           INFO_MEMORY_USAGE_DESC_MAX_PAUSE_TIME.get(),
           maxDetectedPauseTime);
    }

    return Collections.unmodifiableMap(attrs);
  }
}