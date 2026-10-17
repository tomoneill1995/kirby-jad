package com.kirbyjad.character;

public enum Anim
{
	IDLE,
	WALK,
	MAGIC,
	RANGED,
	MELEE,
	DEFEND,
	DEATH,
	HEAL,
	ATTACK,
	// kirby when jad dies
	DANCE;

	public boolean loops()
	{
		return this == IDLE || this == WALK || this == DANCE;
	}
}
