#!/bin/sh
# LibreScoot shell aliases for DBC

# Redis CLI shortcuts (connect to MDB)
alias hgetall='redis-cli -h mdb hgetall'
alias hget='redis-cli -h mdb hget'
alias hset='redis-cli -h mdb hset'
alias hdel='redis-cli -h mdb hdel'
alias publish='redis-cli -h mdb publish'
alias lpush='redis-cli -h mdb lpush'
alias lrange='redis-cli -h mdb lrange'
alias keys='redis-cli -h mdb keys'

# SSH shortcuts
alias mdb='ssh root@mdb'

# Service management
alias svc='lsc svc'
