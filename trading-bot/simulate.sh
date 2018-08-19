#!/bin/bash
set -ue -o pipefail
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..
exec java -Xss515m -cp `cat trading-bot/target/trading-bot-1.0-SNAPSHOT.cp.txt`:`cat trading-gustos/target/trading-gustos-1.0-SNAPSHOT.cp.txt`:trading-bot/target/trading-bot-1.0-SNAPSHOT.jar:trading-gustos/target/trading-gustos-1.0-SNAPSHOT.jar:. -Xdebug -Xrunjdwp:transport=dt_socket,address=3333,server=y,suspend=n  ru.efreet.trading.simulate.Simulate $@
