exclude :test_bug11486, "fix from https://bugs.ruby-lang.org/issues/11486 did not appear to fix it for us"
exclude :test_str_intern, "needs investigation"
exclude :test_str_count, "does not raise compatibility error"
exclude :test_str_crypt_nonstrict, "#crypt failing: Errno::EINVAL: Invalid argument"
