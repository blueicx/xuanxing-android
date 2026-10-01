const mysticGuide = require('../../services/mysticGuide');

function latestTestRecord() {
  try {
    const records = wx.getStorageSync('xuanji_test_records');
    return Array.isArray(records) && records.length ? records[0] : null;
  } catch (e) {
    return null;
  }
}

Component({
  properties: {
    chartFull: { type: Object, value: null },
    fortune: { type: Object, value: null },
  },

  data: {
    mode: 'scholar',
    topic: 'composite',
    topics: mysticGuide.topicLabels(),
    guestChoices: mysticGuide.guestChoices(),
    guide: null,
    activeFollowUp: '',
    evidenceOpen: false,
    turns: [],
    opening: null,
    openingAnswered: false,
    selectedOpening: '',
    pendingOpening: '',
    rhythm: null,
    rhythmAnswered: false,
    selectedRhythm: '',
    pendingRhythm: '',
    moodRhythm: '',
    interactionRound: 0,
    interaction: null,
    thinkingText: '',
    selectedInteraction: null,
    pendingInteraction: '',
    pendingHandoff: '',
    gameCount: 0,
    pendingKey: '',
    askCounts: {},
    pendingCustom: '',
    customQuestion: '',
    customReady: false,
    customCount: 0,
    clarifier: null,
    selectedClarifier: '',
    pendingClarify: '',
    lastInteractionOption: '',
    presenceState: '',
    guestCameo: null,
    selectedGuestChoice: '',
    guestReply: '',
    guestQuestion: '',
    pendingGuest: false,
    guestChoiceCarryoverKey: '',
    pendingGuestChoiceEcho: '',
    memoryNotes: [],
    memorySequence: 0,
    memoryExpanded: false,
  },

  lifetimes: {
    attached() { this.refresh(); },
  },

  observers: {
    'chartFull, fortune'() { this.refresh(); },
  },

  methods: {
    refresh(autoSelectMode = true) {
      const chartFull = this.properties.chartFull;
      const fortune = this.properties.fortune;
      if (!chartFull || !fortune) return;
      let { mode, topic } = this.data;
      if (autoSelectMode) {
        const restored = mysticGuide.readCompanionSession(chartFull, fortune);
        mode = restored.mode;
        topic = restored.topic;
      } else {
        mysticGuide.rememberCompanionSession(chartFull, fortune, mode, topic);
      }
      const guide = mysticGuide.generate(mode, topic, chartFull, fortune, latestTestRecord());
      const preserveRhythm = autoSelectMode === false;
      this.setData({
        mode,
        topic,
        guide,
        activeFollowUp: '',
        evidenceOpen: false,
        turns: [],
        opening: mysticGuide.openingCheckin(mode, topic, guide.styleKey, fortune),
        openingAnswered: false,
        selectedOpening: '',
        pendingOpening: '',
        rhythm: mysticGuide.rhythmCheckin(mode, topic, guide.styleKey, fortune),
        rhythmAnswered: preserveRhythm ? this.data.rhythmAnswered : false,
        selectedRhythm: '',
        pendingRhythm: '',
        moodRhythm: preserveRhythm ? this.data.moodRhythm : '',
        interactionRound: 0,
        interaction: mysticGuide.interaction(mode, topic, fortune, 0),
        thinkingText: '',
        selectedInteraction: null,
        pendingInteraction: '',
        gameCount: 0,
        pendingKey: '',
        askCounts: {},
        pendingCustom: '',
        customQuestion: '',
        customReady: false,
        customCount: 0,
        clarifier: null,
        selectedClarifier: '',
        pendingClarify: '',
        lastInteractionOption: '',
        presenceState: mysticGuide.presenceState(mode, guide.styleKey, 0),
        guestCameo: null,
        selectedGuestChoice: '',
        guestReply: '',
        guestQuestion: '',
        pendingGuest: false,
        guestChoiceCarryoverKey: '',
        pendingGuestChoiceEcho: autoSelectMode === false ? this.data.pendingGuestChoiceEcho : '',
        memoryNotes: autoSelectMode === false ? this.data.memoryNotes : [],
        memorySequence: autoSelectMode === false ? this.data.memorySequence : 0,
        memoryExpanded: autoSelectMode === false ? this.data.memoryExpanded : false,
      });
      return guide;
    },

    appendMemoryNote(kind, detail) {
      const guide = this.data.guide;
      if (!guide) return;
      const text = mysticGuide.memoryNote(this.data.mode, guide.styleKey, kind, detail);
      if (!text) return;
      const note = {
        id: `memory-${kind}-${this.data.memorySequence}`,
        text,
      };
      this.setData({
        memoryNotes: [note, ...this.data.memoryNotes].slice(0, 3),
        memorySequence: this.data.memorySequence + 1,
      });
    },

    onSwitchPersona(e) {
      const mode = e.currentTarget.dataset.mode;
      if (
        !mode ||
        mode === this.data.mode ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;
      this.setData({
        mode,
        pendingHandoff: '',
        clarifier: null,
        selectedClarifier: '',
        pendingClarify: '',
        thinkingText: '',
        guestChoiceCarryoverKey: '',
        pendingGuestChoiceEcho: '',
        memoryNotes: [],
        memorySequence: 0,
        memoryExpanded: false,
      });
      this.refresh(false);
    },

    onSwitchTopic(e) {
      const topic = e.currentTarget.dataset.topic;
      const previousTopic = this.data.topic;
      if (
        !topic ||
        !this.data.guide ||
        topic === previousTopic ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;
      const carryover = mysticGuide.interactionCarryover(
        this.data.mode,
        this.data.guide.styleKey,
        this.data.lastInteractionOption
      );
      const guestChoiceCarryoverKey = this.data.guestChoiceCarryoverKey;
      this.setData({
        topic,
        pendingGuestChoiceEcho: mysticGuide.guestChoiceCarryover(
          this.data.mode,
          this.data.guide.styleKey,
          guestChoiceCarryoverKey
        ),
        guestChoiceCarryoverKey: '',
      });
      const guide = this.refresh(false);
      this.setData({
        pendingHandoff: previousTopic,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, guide.styleKey, 'handoff'),
      });

      setTimeout(() => {
        if (
          this.data.pendingHandoff !== previousTopic ||
          this.data.guide !== guide ||
          this.data.topic !== topic
        ) {
          this.setData({ pendingHandoff: '', thinkingText: '' });
          return;
        }
        const moodPrefix = this.data.moodRhythm
          ? mysticGuide.rhythmCarryover(this.data.mode, guide.styleKey, this.data.moodRhythm)
          : '';
        const guestEcho = this.data.pendingGuestChoiceEcho;
        this.setData({
          turns: [{
            key: `handoff-${previousTopic}-${topic}`,
            question: `刚才在看「${mysticGuide.topicLabel(previousTopic)}」`,
            answer: mysticGuide.topicHandoff(this.data.mode, guide.styleKey, previousTopic, topic),
            reaction: mysticGuide.composeReaction(
              moodPrefix,
              mysticGuide.composeReaction(
                carryover,
                mysticGuide.composeReaction(
                  guestEcho,
                  mysticGuide.handoffReaction(this.data.mode, guide.styleKey)
                )
              )
            ),
            kind: 'handoff',
          }],
          moodRhythm: '',
          pendingGuestChoiceEcho: '',
          presenceState: mysticGuide.presenceState(this.data.mode, guide.styleKey, 1),
          clarifier: null,
          selectedClarifier: '',
          pendingClarify: '',
          guestCameo: null,
          selectedGuestChoice: '',
          guestReply: '',
          guestQuestion: '',
          pendingGuest: false,
          pendingHandoff: '',
          thinkingText: '',
        });
        this.appendMemoryNote('handoff', mysticGuide.topicLabel(previousTopic));
      }, 420);
    },

    onSelectFollowUp(e) {
      const key = e.currentTarget.dataset.key;
      const guide = this.data.guide;
      if (
        !key ||
        !guide ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;
      const oldTurns = this.data.turns;
      const branchIndex = oldTurns.findIndex((turn) => turn.key === key);
      const keptTurns = branchIndex >= 0 ? oldTurns.slice(0, branchIndex) : oldTurns;
      const item = guide.followUps.find((followUp) => followUp.key === key);
      if (!item) return;

      const askedCount = (this.data.askCounts[key] || 0) + 1;
      const action = this.data.activeFollowUp === key ? 'repeat' : (oldTurns.length ? 'branch' : 'ask');
      const carryover = mysticGuide.interactionCarryover(
        this.data.mode,
        guide.styleKey,
        this.data.lastInteractionOption
      );
      const baseReaction = mysticGuide.reaction(this.data.mode, action, askedCount, guide.styleKey);
      this.setData({
        pendingKey: key,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, guide.styleKey, 'ask'),
        lastInteractionOption: '',
      });

      setTimeout(() => {
        if (this.data.pendingKey !== key || this.data.guide !== guide) return;
        const moodPrefix = this.data.moodRhythm
          ? mysticGuide.rhythmCarryover(this.data.mode, guide.styleKey, this.data.moodRhythm)
          : '';
        const guestPrefix = mysticGuide.guestChoiceCarryover(
          this.data.mode,
          guide.styleKey,
          this.data.guestChoiceCarryoverKey
        );
        const combinedReaction = mysticGuide.composeReaction(
          moodPrefix,
          mysticGuide.composeReaction(
            carryover,
            mysticGuide.composeReaction(guestPrefix, baseReaction)
          )
        );
        const repeatLastTurn = action === 'repeat'
          && oldTurns.length > 0
          && oldTurns[oldTurns.length - 1].key === key;
        const turns = repeatLastTurn
          ? oldTurns.map((turn, index) => (
            index === oldTurns.length - 1 ? { ...turn, reaction: combinedReaction } : turn
          ))
          : [...keptTurns, { key, question: item.question, answer: item.answer, reaction: combinedReaction }].slice(-5);
        this.setData({
          turns,
          activeFollowUp: key,
          askCounts: { ...this.data.askCounts, [key]: askedCount },
          moodRhythm: '',
          guestChoiceCarryoverKey: '',
          clarifier: null,
          selectedClarifier: '',
          pendingClarify: '',
          presenceState: mysticGuide.presenceState(this.data.mode, guide.styleKey, turns.length),
          pendingKey: '',
          thinkingText: '',
        });
        this.appendMemoryNote('ask', item.question);
      }, 400);
    },

    onToggleEvidence() {
      this.setData({ evidenceOpen: !this.data.evidenceOpen });
    },

    onToggleMemory() {
      this.setData({ memoryExpanded: !this.data.memoryExpanded });
    },

    onSelectOpening(e) {
      const key = e.currentTarget.dataset.key;
      const guide = this.data.guide;
      const option = (this.data.opening?.options || []).find((item) => item.key === key);
      if (
        !key ||
        !option ||
        !guide ||
        !this.data.opening ||
        this.data.openingAnswered ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;

      this.setData({
        selectedOpening: key,
        pendingOpening: key,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, guide.styleKey, 'opening'),
      });

      setTimeout(() => {
        if (
          this.data.pendingOpening !== key ||
          this.data.guide !== guide ||
          this.data.selectedOpening !== key
        ) {
          this.setData({ pendingOpening: '', thinkingText: '' });
          return;
        }
        const turns = [...this.data.turns, {
          key: `opening-${key}-${guide.topicKey}`,
          question: option.label,
          answer: option.response,
          reaction: mysticGuide.openingReaction(this.data.mode, guide.styleKey),
          kind: 'opening',
        }].slice(-5);
        this.setData({
          turns,
          presenceState: mysticGuide.presenceState(this.data.mode, guide.styleKey, turns.length),
          openingAnswered: true,
          clarifier: null,
          selectedClarifier: '',
          pendingClarify: '',
          pendingOpening: '',
          thinkingText: '',
        });
        this.appendMemoryNote('opening', option.label);
      }, 360);
    },

    onSelectRhythm(e) {
      const key = e.currentTarget.dataset.key;
      const guide = this.data.guide;
      const option = (this.data.rhythm?.options || []).find((item) => item.key === key);
      if (
        !key ||
        !option ||
        !guide ||
        !this.data.rhythm ||
        this.data.rhythmAnswered ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;

      this.setData({
        selectedRhythm: key,
        pendingRhythm: key,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, guide.styleKey, 'rhythm'),
      });

      setTimeout(() => {
        if (
          this.data.pendingRhythm !== key ||
          this.data.guide !== guide ||
          this.data.selectedRhythm !== key ||
          this.data.mode !== guide.mode ||
          this.data.topic !== guide.topicKey ||
          this.data.guide.styleKey !== guide.styleKey
        ) {
          this.setData({ pendingRhythm: '', thinkingText: '', selectedRhythm: '' });
          return;
        }
        const turns = [...this.data.turns, {
          key: `rhythm-${guide.topicKey}-${key}`,
          question: option.label,
          answer: option.response,
          reaction: mysticGuide.rhythmReaction(this.data.mode, guide.styleKey),
          kind: 'rhythm',
        }].slice(-5);
        this.setData({
          turns,
          guestCameo: this.data.guestCameo || mysticGuide.guestCameo(
            this.data.mode,
            guide.topicKey,
            this.properties.fortune,
            key
          ),
          guestReply: '',
          pendingGuest: false,
          presenceState: mysticGuide.presenceState(this.data.mode, guide.styleKey, turns.length),
          rhythmAnswered: true,
          moodRhythm: key,
          clarifier: null,
          selectedClarifier: '',
          pendingClarify: '',
          pendingRhythm: '',
          thinkingText: '',
        });
        this.appendMemoryNote('rhythm', option.label);
      }, 370);
    },

    onSelectGuestReply(e) {
      const key = e.currentTarget.dataset.key;
      const cameo = this.data.guestCameo;
      const guide = this.data.guide;
      const option = mysticGuide.guestChoices().find((item) => item.key === key);
      const rhythmKey = this.data.moodRhythm || '';
      const fortune = this.properties.fortune;
      if (
        !cameo ||
        !guide ||
        !option ||
        !fortune ||
        this.data.guestReply ||
        this.data.selectedGuestChoice ||
        this.data.pendingGuest ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;

      const carryover = mysticGuide.interactionCarryover(
        this.data.mode,
        guide.styleKey,
        this.data.lastInteractionOption
      );

      this.setData({ selectedGuestChoice: key, pendingGuest: true });
      setTimeout(() => {
        if (
          !this.data.pendingGuest ||
          this.data.guestCameo !== cameo ||
          this.data.guide !== guide ||
          this.data.mode !== guide.mode ||
          this.data.topic !== guide.topicKey ||
          this.data.guide.styleKey !== guide.styleKey ||
          this.properties.fortune !== fortune ||
          this.data.selectedGuestChoice !== key
        ) {
          if (this.data.pendingGuest) this.setData({ pendingGuest: false, selectedGuestChoice: '' });
          return;
        }
        const guestAnswer = mysticGuide.guestReply(
          this.data.mode,
          guide.topicKey,
          fortune,
          rhythmKey,
          key
        );
        const hostAnswer = mysticGuide.guestHostWrapup(
          this.data.mode,
          guide.styleKey,
          guide.topicKey,
          fortune,
          rhythmKey,
          key
        );
        const moodPrefix = mysticGuide.rhythmCarryover(this.data.mode, guide.styleKey, rhythmKey);
        const turns = [...this.data.turns, {
          key: `guest-${guide.topicKey}-${key}`,
          question: option.label,
          answer: hostAnswer,
          reaction: mysticGuide.composeReaction(moodPrefix, carryover),
          kind: 'ask',
        }].slice(-5);
        this.setData({
          turns,
          guestQuestion: option.label,
          guestReply: guestAnswer,
          presenceState: mysticGuide.presenceState(this.data.mode, guide.styleKey, turns.length),
          moodRhythm: '',
          lastInteractionOption: '',
          guestChoiceCarryoverKey: key,
          clarifier: null,
          selectedClarifier: '',
          pendingClarify: '',
          pendingGuest: false,
        });
        this.appendMemoryNote('guest', option.label);
      }, 320);
    },

    onCustomInput(e) {
      const value = String(e.detail.value || '').slice(0, 60);
      this.setData({ customQuestion: value, customReady: Boolean(value.trim()) });
    },

    onSubmitCustom() {
      const guide = this.data.guide;
      const question = String(this.data.customQuestion || '').trim().slice(0, 60);
      if (
        !question ||
        !guide ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;
      const carryover = mysticGuide.interactionCarryover(
        this.data.mode,
        guide.styleKey,
        this.data.lastInteractionOption
      );
      this.setData({
        customQuestion: question,
        pendingCustom: question,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, this.data.guide.styleKey, 'ask'),
        lastInteractionOption: '',
      });

      setTimeout(() => {
        if (this.data.pendingCustom !== question || this.data.guide !== guide) return;
        const moodPrefix = this.data.moodRhythm
          ? mysticGuide.rhythmCarryover(this.data.mode, guide.styleKey, this.data.moodRhythm)
          : '';
        const guestPrefix = mysticGuide.guestChoiceCarryover(
          this.data.mode,
          guide.styleKey,
          this.data.guestChoiceCarryoverKey
        );
        const fortune = this.properties.fortune;
        const answer = mysticGuide.customAnswer(
          this.data.mode,
          guide.topicKey,
          question,
          fortune,
          latestTestRecord()
        );
        const clarifier = mysticGuide.customClarifier(
          this.data.mode,
          guide.styleKey,
          question,
          fortune,
          latestTestRecord()
        );
        const turns = [...this.data.turns, {
          key: `custom-${this.data.customCount}-${question}`,
          question,
          answer,
          reaction: mysticGuide.composeReaction(
            moodPrefix,
            mysticGuide.composeReaction(
              carryover,
              mysticGuide.composeReaction(
                guestPrefix,
                mysticGuide.customReaction(this.data.mode, guide.styleKey, question)
              )
            )
          ),
          kind: 'custom',
        }].slice(-5);
        this.setData({
          turns,
          presenceState: mysticGuide.presenceState(this.data.mode, guide.styleKey, turns.length),
          customQuestion: '',
          customCount: this.data.customCount + 1,
          clarifier,
          selectedClarifier: '',
          pendingClarify: '',
          moodRhythm: '',
          guestChoiceCarryoverKey: '',
          pendingCustom: '',
          thinkingText: '',
        });
        this.appendMemoryNote('ask', question);
      }, 430);
    },

    onSelectClarifier(e) {
      const key = e.currentTarget.dataset.key;
      const guide = this.data.guide;
      const fortune = this.properties.fortune;
      const clarifier = this.data.clarifier;
      const sourceTurn = this.data.turns[this.data.turns.length - 1];
      const option = (clarifier?.options || []).find((item) => item.key === key);
      if (
        !key ||
        !option ||
        !guide ||
        !fortune ||
        !clarifier ||
        !sourceTurn ||
        sourceTurn.kind !== 'custom' ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingGuest ||
        this.data.pendingClarify ||
        this.data.selectedClarifier
      ) return;

      this.setData({
        selectedClarifier: key,
        pendingClarify: key,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, guide.styleKey, 'ask'),
      });

      setTimeout(() => {
        if (
          this.data.pendingClarify !== key ||
          this.data.selectedClarifier !== key ||
          this.data.guide !== guide ||
          this.data.clarifier !== clarifier ||
          this.properties.fortune !== fortune ||
          this.data.turns[this.data.turns.length - 1] !== sourceTurn
        ) {
          if (this.data.pendingClarify === key) {
            this.setData({ pendingClarify: '', selectedClarifier: '', thinkingText: '' });
          }
          return;
        }

        const turns = [...this.data.turns, {
          key: `clarify-${this.data.customCount - 1}-${key}`,
          question: option.label,
          answer: option.answer,
          reaction: mysticGuide.customReaction(this.data.mode, guide.styleKey, option.label),
          kind: 'clarify',
        }].slice(-5);
        this.setData({
          turns,
          presenceState: mysticGuide.presenceState(this.data.mode, guide.styleKey, turns.length),
          clarifier: null,
          selectedClarifier: '',
          pendingClarify: '',
          thinkingText: '',
        });
      }, 370);
    },

    onResetConversation() {
      this.setData({
        turns: [],
        activeFollowUp: '',
        pendingKey: '',
        pendingInteraction: '',
        pendingHandoff: '',
        pendingCustom: '',
        clarifier: null,
        selectedClarifier: '',
        pendingClarify: '',
        customQuestion: '',
        customReady: false,
        opening: mysticGuide.openingCheckin(
          this.data.mode,
          this.data.topic,
          this.data.guide?.styleKey || '',
          this.properties.fortune
        ),
        openingAnswered: false,
        selectedOpening: '',
        pendingOpening: '',
        rhythm: mysticGuide.rhythmCheckin(
          this.data.mode,
          this.data.topic,
          this.data.guide?.styleKey || '',
          this.properties.fortune
        ),
        rhythmAnswered: false,
        selectedRhythm: '',
        pendingRhythm: '',
        moodRhythm: '',
        selectedGuestChoice: '',
        guestQuestion: '',
        memoryNotes: [],
        memorySequence: 0,
        memoryExpanded: false,
        selectedInteraction: null,
        lastInteractionOption: '',
        guestCameo: null,
        guestReply: '',
        pendingGuest: false,
        guestChoiceCarryoverKey: '',
        pendingGuestChoiceEcho: '',
        presenceState: mysticGuide.presenceState(this.data.mode, this.data.guide?.styleKey || '', 0),
        thinkingText: '',
        askCounts: {},
        gameCount: 0,
      });
    },

    onSelectInteraction(e) {
      const { label } = e.currentTarget.dataset;
      const option = (this.data.interaction?.options || []).find((item) => item.label === label);
      if (
        !option ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;
      const guide = this.data.guide;
      const round = this.data.interactionRound || 0;
      const interactionCarryover = mysticGuide.interactionCarryover(
        this.data.mode,
        guide.styleKey,
        this.data.lastInteractionOption
      );
      this.setData({
        selectedInteraction: option,
        pendingInteraction: label,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, guide.styleKey, 'game'),
      });

      setTimeout(() => {
        if (
          this.data.pendingInteraction !== label ||
          this.data.guide !== guide ||
          this.data.interactionRound !== round ||
          this.data.selectedInteraction !== option
        ) {
          this.setData({ pendingInteraction: '', thinkingText: '' });
          return;
        }
        const moodPrefix = this.data.moodRhythm
          ? mysticGuide.rhythmCarryover(this.data.mode, guide.styleKey, this.data.moodRhythm)
          : '';
        const guestPrefix = mysticGuide.guestChoiceCarryover(
          this.data.mode,
          guide.styleKey,
          this.data.guestChoiceCarryoverKey
        );
        const baseReaction = mysticGuide.interactionReaction(this.data.mode, guide.styleKey);
        const turns = [...this.data.turns, {
          key: `game-${this.data.gameCount}-${label}`,
          question: option.label,
          answer: option.feedback,
          reaction: mysticGuide.composeReaction(
            moodPrefix,
            mysticGuide.composeReaction(
              interactionCarryover,
              mysticGuide.composeReaction(
                guestPrefix,
                baseReaction
              )
            )
          ),
          kind: 'game',
        }].slice(-5);
        this.setData({
          turns,
          pendingInteraction: '',
          thinkingText: '',
          moodRhythm: '',
          lastInteractionOption: option.label,
          guestChoiceCarryoverKey: '',
          clarifier: null,
          selectedClarifier: '',
          pendingClarify: '',
          presenceState: mysticGuide.presenceState(this.data.mode, guide.styleKey, turns.length),
          gameCount: this.data.gameCount + 1,
        });
        this.appendMemoryNote('game', option.label);
      }, 380);
    },

    onNextInteraction() {
      if (
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom ||
        this.data.pendingOpening ||
        this.data.pendingRhythm ||
        this.data.pendingClarify ||
        this.data.selectedClarifier ||
        this.data.pendingGuest
      ) return;
      const round = ((this.data.interactionRound || 0) + 1);
      this.setData({
        interactionRound: round,
        interaction: mysticGuide.interaction(this.data.mode, this.data.topic, this.properties.fortune, round),
        selectedInteraction: null,
        pendingInteraction: '',
        pendingHandoff: '',
        thinkingText: '',
      });
    },
  },
});
