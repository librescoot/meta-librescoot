#!/bin/sh
# Librescoot shell aliases

# Redis CLI shortcuts
alias hgetall='redis-cli hgetall'
alias hget='redis-cli hget'
alias hset='redis-cli hset'
alias hdel='redis-cli hdel'
alias publish='redis-cli publish'
alias lpush='redis-cli lpush'
alias lrange='redis-cli lrange'
alias keys='redis-cli keys'

# SSH shortcuts
alias dbc='ssh root@dbc'

# Service management
alias svc='lsc svc'
