package skills

import (
	"fmt"
	"io/fs"
	"log"
	"path/filepath"
	"strings"

	"github.com/gentleman-programming/gentle-ai/internal/agents"
	"github.com/gentleman-programming/gentle-ai/internal/assets"
	"github.com/gentleman-programming/gentle-ai/internal/components/filemerge"
	"github.com/gentleman-programming/gentle-ai/internal/model"
)

// isSDDSkill reports whether a skill ID belongs to the SDD orchestrator suite.
// SDD skills are installed by the SDD component; the skills component skips
// them to prevent duplicate writes when both components are selected.
func isSDDSkill(id model.SkillID) bool {
	return strings.HasPrefix(string(id), "sdd-")
}

type InjectionResult struct {
	Changed bool
	Files   []string
	Skipped []model.SkillID
}

// Inject writes the embedded skill directory for each requested skill to the
// correct directory for the given agent adapter. This includes SKILL.md and any
// supporting files such as assets/ templates.
//
// The skills directory is determined by adapter.SkillsDir(), removing the need
// for any agent-specific switch statements.
//
// SDD skills (those whose IDs begin with "sdd-") are intentionally skipped here
// because the SDD component installs them as part of its own injection. This
// prevents a write conflict when both components are selected together.
//
// Individual skill failures (e.g., missing embedded asset) are logged and
// skipped rather than aborting the entire operation.
func Inject(homeDir string, adapter agents.Adapter, skillIDs []model.SkillID) (InjectionResult, error) {
	if !adapter.SupportsSkills() {
		return InjectionResult{Skipped: skillIDs}, nil
	}

	skillDir := adapter.SkillsDir(homeDir)
	if skillDir == "" {
		return InjectionResult{Skipped: skillIDs}, nil
	}

	paths := make([]string, 0, len(skillIDs))
	skipped := make([]model.SkillID, 0)
	changed := false

	for _, id := range skillIDs {
		// SDD skills are written by the SDD component — skip to avoid conflicts.
		if isSDDSkill(id) {
			continue
		}

		assetRoot := "skills/" + string(id)
		if _, readErr := assets.FS.ReadDir(assetRoot); readErr != nil {
			log.Printf("skills: skipping %q — embedded asset directory not found: %v", id, readErr)
			skipped = append(skipped, id)
			continue
		}

		written, skillChanged, writeErr := writeEmbeddedSkillDir(assetRoot, filepath.Join(skillDir, string(id)))
		if writeErr != nil {
			return InjectionResult{}, fmt.Errorf("skill %q: write failed: %w", id, writeErr)
		}

		changed = changed || skillChanged
		paths = append(paths, written...)
	}

	return InjectionResult{Changed: changed, Files: paths, Skipped: skipped}, nil
}

func writeEmbeddedSkillDir(assetRoot, targetRoot string) ([]string, bool, error) {
	written := make([]string, 0)
	changed := false
	sawSkillFile := false

	err := fs.WalkDir(assets.FS, assetRoot, func(walkPath string, d fs.DirEntry, walkErr error) error {
		if walkErr != nil {
			return walkErr
		}
		if d.IsDir() {
			return nil
		}

		rel := strings.TrimPrefix(walkPath, assetRoot+"/")
		if rel == "SKILL.md" {
			sawSkillFile = true
		}

		content, err := assets.FS.ReadFile(walkPath)
		if err != nil {
			return err
		}
		if rel == "SKILL.md" && len(content) == 0 {
			return fmt.Errorf("embedded SKILL.md exists but is empty — build may be corrupt")
		}

		targetPath := filepath.Join(targetRoot, filepath.FromSlash(rel))
		writeResult, err := filemerge.WriteFileAtomic(targetPath, content, 0o644)
		if err != nil {
			return err
		}

		changed = changed || writeResult.Changed
		written = append(written, targetPath)
		return nil
	})
	if err != nil {
		return nil, false, err
	}
	if !sawSkillFile {
		return nil, false, fmt.Errorf("embedded skill directory missing SKILL.md")
	}
	return written, changed, nil
}

// SkillPathForAgent returns the filesystem path where a skill file would be written.
func SkillPathForAgent(homeDir string, adapter agents.Adapter, id model.SkillID) string {
	skillDir := adapter.SkillsDir(homeDir)
	if skillDir == "" {
		return ""
	}
	return filepath.Join(skillDir, string(id), "SKILL.md")
}
