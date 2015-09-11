#
# Configuration for get_content.sh
#

export DATA="data_rhea"

# The Solr installation 
export SOLR="http://rhea:56708/aviser/sbsolr/collection1"

# Year range is inclusive for start and end
export YEAR_START=2001
export YEAR_END=2002

# Limiting of search space
ROWS=100
export SOLR_PARAM="rows=${ROWS}&fl=timestamp,fulltext_org&facet=false&hl=false"
# Expected to be a Solr tdate (2015-09-11T10:49:00Z)
export DATE_FIELD="timestamp"
