local group1 = {
    {566, {1,2,3,4,5}},
    {566, {1,2,3,4,5}},
    {566, {1,2,3,4,5}},
    {566, {1,2,3,4,5}},
    {566, {1,2,3,4,5}}
}

local group2 = {
    {578, {1,2,3,4,5}},
    {578, {1,2,3,4,5}},
    {578, {1,2,3,4,5}},
    {578, {1,2,3,4,5}}
}

local group3 = {
    {578, {1,2,3,4,5}},
    {566, {1,2,3,4,5}},
    {566, {1,2,3,4,5}},
    {566, {1,2,3,4,5}},
    {566, {1,2,3,4,5}},
    {566, {1,2,3,4,5}}
}

local group4 = {
    {579, {1,2,3,4,5}},
    {579, {1,2,3,4,5}},
    {578, {1,2,3,4,5}},
    {578, {1,2,3,4,5}},
	{578, {1,2,3,4,5}},
	{578, {1,2,3,4,5}},
    {578, {1,2,3,4,5}}
}

local group5 = {
    {581, {1,2,3,4,5}},
    {579, {1,2,3,4,5}},
    {579, {1,2,3,4,5}},
    {579, {1,2,3,4,5}},
    {579, {1,2,3,4,5}},
	{578, {1,2,3,4,5}}
}

local group6 = {
    {580, {1,2,3,4,5}},
    {580, {1,2,3,4,5}},
    {579, {1,2,3,4,5}},
    {578, {1,2,3,4,5}},
    {578, {1,2,3,4,5}},
	{578, {1,2,3,4,5}},
	{578, {1,2,3,4,5}}
}

local group7 = {
    {580, {1,2,3,4,5}},
    {581, {1,2,3,4,5}},
    {581, {1,2,3,4,5}},
    {581, {1,2,3,4,5}},
    {579, {1,2,3,4,5}},
	{579, {1,2,3,4,5}},
	{578, {1,2,3,4,5}},
	{578, {1,2,3,4,5}}
}

local group8 = {
    {607, {1,2,3,4,5}},
    {607, {1,2,3,4,5}},
    {607, {1,2,3,4,5}},
    {607, {1,2,3,4,5}}
}

local group9 = {
    {612, {1,2,3,4,5}},
    {580, {1,2,3,4,5}},
    {581, {1,2,3,4,5}},
    {579, {1,2,3,4,5}};
	{578, {1,2,3,4,5}},
    {578, {1,2,3,4,5}},
}

local mapInfos = {
    [8282] = {groupCell= 267, group= group1, winDest= {8321,433}},
    [8321] = {groupCell= 315, group= group2, winDest= {8322,433}},
    [8322] = {groupCell= 292, group= group3, winDest= {8326,433}},
    [8326] = {groupCell= 386, group= group4, winDest= {8327,433}},
    [8327] = {groupCell= 324, group= group5, winDest= {8328,433}},
	[8328] = {groupCell= 329, group= group6, winDest= {8329,433}},
	[8329] = {groupCell= 205, group= group7, winDest= {8330,231}},
	[8330] = {groupCell= 239, group= group8, winDest= {8331,273}},
    [8331] = {groupCell= 237, group= group9, winDest= {8497,42}}
}

for k,v in pairs(mapInfos) do
    local map = MAPS[k]
    if map then
        map.onFightEnd[PVMFightType] = fightEndTeleportWinnerPlayers(v.winDest[1], v.winDest[2])
        map.staticGroups = {
            {v.groupCell, v.group}
        }
    end
end