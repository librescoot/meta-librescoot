#!/bin/sh
# Librescoot shell aliases for DBC

# Default redis-cli to connect to MDB
alias redis-cli='redis-cli -h mdb'

# Redis CLI shortcuts (connect to MDB)
alias hgetall='redis-cli hgetall'
alias hget='redis-cli hget'
alias hset='redis-cli hset'
alias hdel='redis-cli hdel'
alias publish='redis-cli publish'
alias lpush='redis-cli lpush'
alias lrange='redis-cli lrange'
alias keys='redis-cli keys'

# SSH shortcuts
alias mdb='ssh root@mdb'

# Service management
alias svc='lsc svc'
