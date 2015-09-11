#!/bin/bash

#
# Connects to a Solr server and samples content from time-based queries
# X samples are taken from each month
#

# TODO: Consider adding paging to get large result sets

source "settings.sh"

# Expects DATE_QUERY and DATE_PREFIX
function get_specific() {
    local OUT="${DATA}/${DATE_PREFIX}.log"
    if [ -s "$OUT" ]; then
        return
    fi
    local URL="$SOLR/select?wt=json&indent=true&${SOLR_PARAM}&q=${DATE_QUERY}"
    wget "$URL" -O ${DATA}/${DATE_PREFIX}.log > /dev/null 2> /dev/null
}

function get_all() {
    echo "Retrieving samples for year $YEAR_START to $YEAR_END"
    for YEAR in `seq $YEAR_START $YEAR_END`; do
        echo " - $YEAR"
        for MONTH in `seq 1 12`; do
            DATE_PREFIX="${YEAR}-${MONTH}"

            if [ "$MONTH" -eq "12" ]; then
                local END="$((YEAR+1))-01"
            elif [ "$MONTH" -lt "9" ]; then
                local END="${YEAR}-0$((MONTH+1))"
            else 
                local END="${YEAR}-$((MONTH+1))"
            fi

            if [ "$MONTH" -lt "10" ]; then
                DATE_PREFIX="${YEAR}-0${MONTH}"
            fi

            DATE_QUERY="${DATE_FIELD}:[${DATE_PREFIX}-01T00:00:00Z TO ${END}-01T00:00:00Z]"
            get_specific
        done
    done
}

function init() {
    if [ ! -d "$DATA" ]; then
        echo "Creating data folder $DATA"
        mkdir -p "$DATA"
    else
        echo "Data folder $DATA already exists. Old results will be re-used when possible"
    fi
}

# Expects LOG
function clean_specific() {
    local BASE="${LOG%.*}"
    local DAT="${BASE}.dat"
    if [ -s "$DAT" ]; then
        return
    fi
    cat "$LOG" | grep -A 9999999 "fulltext_org\":" | grep -m 1 -B 9999999 "  }}$" | grep "\"" | grep -v "timestamp.:" | grep -v "},$" | sed -e 's/"fulltext_org":\[//' -e 's/[ ]*"//' -e 's/",$//' -e 's/\n/ /' -e 's/"],$//' -e 's/\\//g' | tr '\n' ' ' > "$DAT"
}

function clean_data() {
    pushd "$DATA" > /dev/null
    for LOG in *.log; do
        clean_specific $LOG
    done
}

init
get_all
clean_data
