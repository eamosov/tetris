#!/bin/bash
set -ue -o pipefail
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..
exec chrt  -i 0 java -cp `cat trading-bot/target/trading-bot-1.0-SNAPSHOT.cp.txt`:$DIR/..:trading-bot/target/trading-bot-1.0-SNAPSHOT.jar  ru.efreet.trading.bot.Main $@
