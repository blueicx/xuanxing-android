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
    guide: null,
    activeFollowUp: '',
    evidenceOpen: false,
    turns: [],
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
      const { mode, topic } = this.data;
      const nextMode = autoSelectMode ? mysticGuide.suggestedMode(topic, fortune) : mode;
      const guide = mysticGuide.generate(nextMode, topic, chartFull, fortune, latestTestRecord());
      this.setData({
        mode: nextMode,
        guide,
        activeFollowUp: '',
        evidenceOpen: false,
        turns: [],
        interactionRound: 0,
        interaction: mysticGuide.interaction(nextMode, topic, fortune, 0),
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
      });
      return guide;
    },

    onSwitchPersona(e) {
      const mode = e.currentTarget.dataset.mode;
      if (!mode || mode === this.data.mode) return;
      this.setData({ mode, pendingHandoff: '', thinkingText: '' });
      this.refresh(false);
    },

    onSwitchTopic(e) {
      const topic = e.currentTarget.dataset.topic;
      const previousTopic = this.data.topic;
      if (
        !topic ||
        topic === previousTopic ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom
      ) return;
      this.setData({ topic });
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
        this.setData({
          turns: [{
            key: `handoff-${previousTopic}-${topic}`,
            question: `刚才在看「${mysticGuide.topicLabel(previousTopic)}」`,
            answer: mysticGuide.topicHandoff(this.data.mode, guide.styleKey, previousTopic, topic),
            reaction: mysticGuide.handoffReaction(this.data.mode, guide.styleKey),
            kind: 'handoff',
          }],
          pendingHandoff: '',
          thinkingText: '',
        });
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
        this.data.pendingCustom
      ) return;
      const oldTurns = this.data.turns;
      const branchIndex = oldTurns.findIndex((turn) => turn.key === key);
      const keptTurns = branchIndex >= 0 ? oldTurns.slice(0, branchIndex) : oldTurns;
      const item = guide.followUps.find((followUp) => followUp.key === key);
      if (!item) return;

      const askedCount = (this.data.askCounts[key] || 0) + 1;
      const action = this.data.activeFollowUp === key ? 'repeat' : (oldTurns.length ? 'branch' : 'ask');
      const reactionLine = mysticGuide.reaction(this.data.mode, action, askedCount, guide.styleKey);
      this.setData({
        pendingKey: key,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, guide.styleKey, 'ask'),
      });

      setTimeout(() => {
        if (this.data.pendingKey !== key || this.data.guide !== guide) return;
        const turns = action === 'repeat'
          ? oldTurns.map((turn, index) => (
            index === oldTurns.length - 1 ? { ...turn, reaction: reactionLine } : turn
          ))
          : [...keptTurns, { key, question: item.question, answer: item.answer, reaction: reactionLine }].slice(-5);
        this.setData({
          turns,
          activeFollowUp: key,
          askCounts: { ...this.data.askCounts, [key]: askedCount },
          pendingKey: '',
          thinkingText: '',
        });
      }, 400);
    },

    onToggleEvidence() {
      this.setData({ evidenceOpen: !this.data.evidenceOpen });
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
        this.data.pendingCustom
      ) return;
      this.setData({
        customQuestion: question,
        pendingCustom: question,
        thinkingText: mysticGuide.thinkingLine(this.data.mode, this.data.guide.styleKey, 'ask'),
      });

      setTimeout(() => {
        if (this.data.pendingCustom !== question || this.data.guide !== guide) return;
        this.setData({
          turns: [...this.data.turns, {
            key: `custom-${this.data.customCount}-${question}`,
            question,
            answer: mysticGuide.customAnswer(
              this.data.mode,
              guide.topicKey,
              question,
              this.properties.fortune,
              latestTestRecord()
            ),
            reaction: mysticGuide.customReaction(this.data.mode, guide.styleKey, question),
            kind: 'ask',
          }].slice(-5),
          customQuestion: '',
          customCount: this.data.customCount + 1,
          pendingCustom: '',
          thinkingText: '',
        });
      }, 430);
    },

    onResetConversation() {
      this.setData({
        turns: [],
        activeFollowUp: '',
        pendingKey: '',
        pendingInteraction: '',
        pendingHandoff: '',
        pendingCustom: '',
        customQuestion: '',
        customReady: false,
        selectedInteraction: null,
        thinkingText: '',
        askCounts: {},
        gameCount: 0,
      });
    },

    onSelectInteraction(e) {
      const { label } = e.currentTarget.dataset;
      const option = (this.data.interaction?.options || []).find((item) => item.label === label);
      if (!option || this.data.pendingKey || this.data.pendingInteraction || this.data.pendingHandoff) return;
      if (
        !option ||
        this.data.pendingKey ||
        this.data.pendingInteraction ||
        this.data.pendingHandoff ||
        this.data.pendingCustom
      ) return;
      const guide = this.data.guide;
      const round = this.data.interactionRound || 0;
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
        this.setData({
          turns: [...this.data.turns, {
            key: `game-${this.data.gameCount}-${label}`,
            question: option.label,
            answer: option.feedback,
            reaction: mysticGuide.interactionReaction(this.data.mode, guide.styleKey),
            kind: 'game',
          }].slice(-5),
          pendingInteraction: '',
          thinkingText: '',
          gameCount: this.data.gameCount + 1,
        });
      }, 380);
    },

    onNextInteraction() {
      const round = ((this.data.interactionRound || 0) + 1);
      this.setData({
        interactionRound: round,
        interaction: mysticGuide.interaction(this.data.mode, this.data.topic, this.properties.fortune, round),
        selectedInteraction: null,
        pendingInteraction: '',
        pendingHandoff: '',
        thinkingText: '',
        gameCount: 0,
      });
    },
  },
});
