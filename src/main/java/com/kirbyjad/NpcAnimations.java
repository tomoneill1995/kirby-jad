package com.kirbyjad;

import com.kirbyjad.character.Anim;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.AnimationID;

// fight caves npc animations -> the toon animation that plays for them. ids are unique per monster
// so one table covers everything
final class NpcAnimations
{
	private static final Map<Integer, Anim> ANIMS = new HashMap<>();

	static
	{
		// jad
		ANIMS.put(AnimationID.LORDMAGMUS_FIRE, Anim.MAGIC);
		ANIMS.put(AnimationID.LORDMAGMUS_SMASH, Anim.RANGED);
		ANIMS.put(AnimationID.LORDMAGMUS_ATTACK, Anim.MELEE);
		ANIMS.put(AnimationID.LORDMAGMUS_DEFEND, Anim.DEFEND);
		ANIMS.put(AnimationID.LORDMAGMUS_DEATH, Anim.DEATH);

		// jad healers and yt-mejkot
		ANIMS.put(AnimationID.LIZARD_CLERIC_HEAL, Anim.HEAL);
		ANIMS.put(AnimationID.LIZARD_CLERIC_ATTACK, Anim.ATTACK);
		ANIMS.put(AnimationID.LIZARD_CLERIC_DEFEND, Anim.DEFEND);
		ANIMS.put(AnimationID.LIZARD_CLERIC_DEATH, Anim.DEATH);

		// tz-kih
		ANIMS.put(AnimationID.FIREBAT_ATTACK, Anim.ATTACK);
		ANIMS.put(AnimationID.FIREBAT_DEFEND, Anim.DEFEND);
		ANIMS.put(AnimationID.FIREBAT_DEATH, Anim.DEATH);

		// tz-kek
		ANIMS.put(AnimationID.LAVABEAST_ATTACK, Anim.ATTACK);
		ANIMS.put(AnimationID.LAVABEAST_DEFEND, Anim.DEFEND);
		ANIMS.put(AnimationID.LAVABEAST_DEATH, Anim.DEATH);

		// tok-xil
		ANIMS.put(AnimationID.MAGMAQURIS_SPINE_ATTACK, Anim.RANGED);
		ANIMS.put(AnimationID.MAGMAQURIS_PUNCH, Anim.MELEE);
		ANIMS.put(AnimationID.MAGMAQURIS_DEFEND, Anim.DEFEND);
		ANIMS.put(AnimationID.MAGMAQURIS_DEATH, Anim.DEATH);

		// ket-zek
		ANIMS.put(AnimationID.IGNIFERUM_RANGED, Anim.MAGIC);
		ANIMS.put(AnimationID.IGNIFERUM_ATTACK, Anim.MELEE);
		ANIMS.put(AnimationID.IGNIFERUM_DEFEND, Anim.DEFEND);
		ANIMS.put(AnimationID.IGNIFERUM_DEATH, Anim.DEATH);
	}

	private NpcAnimations()
	{
	}

	static Anim map(int id)
	{
		return ANIMS.get(id);
	}
}
