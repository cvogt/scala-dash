#!/bin/sh
rsync -avz --delete --exclude '.DS_Store' --exclude '.htaccess' --progress docsets/ ~/domainfactory/webseiten/cvogt.org/releases/docsets/