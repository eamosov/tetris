#!/bin/bash
set -ue -o pipefail
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR
exec java -cp `cat trading-common/target/trading-common-1.0-SNAPSHOT.cp.txt`:trading-common/target/trading-common-1.0-SNAPSHOT.jar  ru.efreet.trading.exchange.impl.cache.UpdateOne $@
